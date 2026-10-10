package com.andriybobchuk.mooney.mooney.presentation.screenshot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.CommitScreenshotImportUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParsedCandidate
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.RunScreenshotImportUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One row in the preview list — pairs a parsed candidate with the user's
 * accept/reject toggle. We keep an explicit [accepted] field (rather than
 * deriving from status) because the user can override a DUP_EXISTING back
 * to accepted via the "keep anyway" affordance.
 */
data class CandidateRow(
    val candidate: ParsedCandidate,
    val accepted: Boolean
)

data class ScreenshotImportState(
    /** Rows the user is reviewing. Empty while picker is open or on first
     *  render; a load call populates this. */
    val rows: List<CandidateRow> = emptyList(),
    /** True from invocation of pickAndImport() until OCR + parse returns.
     *  SwiftUI PhotosPicker blocks the suspend, so this covers "picker is
     *  up" AND "OCR running after picker close". */
    val isPicking: Boolean = false,
    /** True while [CommitScreenshotImportUseCase] is inserting rows. */
    val isCommitting: Boolean = false,
    /** User cancelled the picker (empty return). Terminal — the screen
     *  should pop back to Transactions. */
    val isCancelled: Boolean = false,
    /** Commit finished; emitted alongside the count so the Transactions
     *  tab can show a toast. */
    val completedInserted: Int? = null,
    /** True once the picker returned at least one image, regardless of
     *  parse outcome. Gates the preview UI. */
    val hasPickedImages: Boolean = false,
    val errorMessage: String? = null
) {
    val acceptedCount: Int get() = rows.count { it.accepted }
    val canCommit: Boolean get() = acceptedCount > 0 && !isCommitting && !isPicking
}

sealed interface ScreenshotImportAction {
    /** Fires on screen entry — launches the picker + OCR + parse pipeline. */
    data object Load : ScreenshotImportAction
    /** Toggle a single row on/off. */
    data class ToggleRow(val index: Int) : ScreenshotImportAction
    /** Bulk-toggle everything that currently matches a given accepted state. */
    data class ToggleAll(val accepted: Boolean) : ScreenshotImportAction
    /** Flip a dup to accepted (user overriding our recommendation). */
    data class KeepAnyway(val index: Int) : ScreenshotImportAction
    /** Commit all accepted rows. */
    data object Commit : ScreenshotImportAction
    /** User tapped close / back. Fires cancellation telemetry if nothing
     *  was committed. */
    data object Dismiss : ScreenshotImportAction
}

sealed interface ScreenshotImportEvent {
    /** Pop back to Transactions and show a toast. */
    data class Finished(val insertedCount: Int) : ScreenshotImportEvent
    /** Pop back to Transactions silently (cancel / no selection). */
    data object DismissedEmpty : ScreenshotImportEvent
}

class ScreenshotImportViewModel(
    private val runScreenshotImportUseCase: RunScreenshotImportUseCase,
    private val commitScreenshotImportUseCase: CommitScreenshotImportUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(ScreenshotImportState())
    val state: StateFlow<ScreenshotImportState> = _state.asStateFlow()

    /** One-shot nav effects (Finished, DismissedEmpty). Channel so collisions
     *  with a slow collector don't drop the signal. */
    private val _events = Channel<ScreenshotImportEvent>(BUFFERED)
    val events: Flow<ScreenshotImportEvent> = _events.receiveAsFlow()

    /** True once Commit succeeded — used by Dismiss to skip firing the
     *  Cancelled analytics event (we already fired Committed). */
    private var hasCommitted: Boolean = false

    fun onAction(action: ScreenshotImportAction) {
        when (action) {
            ScreenshotImportAction.Load -> loadPicker()
            is ScreenshotImportAction.ToggleRow -> toggleRow(action.index)
            is ScreenshotImportAction.ToggleAll -> toggleAll(action.accepted)
            is ScreenshotImportAction.KeepAnyway -> toggleRow(action.index, forceAccepted = true)
            ScreenshotImportAction.Commit -> commit()
            ScreenshotImportAction.Dismiss -> dismiss()
        }
    }

    private fun loadPicker() {
        if (_state.value.hasPickedImages || _state.value.isPicking) return
        analyticsTracker.trackEvent(AnalyticsEvent.ScreenshotImportOpened)
        _state.update { it.copy(isPicking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val candidates = runScreenshotImportUseCase()
                if (candidates.isEmpty()) {
                    _state.update { it.copy(isPicking = false, isCancelled = true) }
                    _events.trySend(ScreenshotImportEvent.DismissedEmpty)
                    return@launch
                }

                // Default accept rule: NEW rows on, dup/parse-failed rows off.
                // Preserves the Phase 3 spec — user explicitly asked that dups
                // show "keep anyway" as an opt-in, not an opt-out.
                val rows = candidates.map { c ->
                    CandidateRow(
                        candidate = c,
                        accepted = c.status == ParsedCandidate.Status.NEW
                    )
                }

                emitParsedTelemetry(candidates)

                _state.update {
                    it.copy(
                        rows = rows,
                        isPicking = false,
                        hasPickedImages = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPicking = false,
                        errorMessage = e.message ?: "Couldn't read screenshots"
                    )
                }
            }
        }
    }

    private fun toggleRow(index: Int, forceAccepted: Boolean? = null) {
        _state.update { s ->
            val row = s.rows.getOrNull(index) ?: return@update s
            if (row.candidate.status == ParsedCandidate.Status.PARSE_FAILED) {
                // Parse-failed rows aren't actionable — ignore toggles.
                return@update s
            }
            val newAccepted = forceAccepted ?: !row.accepted
            s.copy(
                rows = s.rows.toMutableList().apply {
                    this[index] = row.copy(accepted = newAccepted)
                }
            )
        }
    }

    private fun toggleAll(accepted: Boolean) {
        _state.update { s ->
            s.copy(
                rows = s.rows.map { row ->
                    if (row.candidate.status == ParsedCandidate.Status.PARSE_FAILED) {
                        row
                    } else {
                        row.copy(accepted = accepted)
                    }
                }
            )
        }
    }

    private fun commit() {
        val snapshot = _state.value
        if (!snapshot.canCommit) return
        val accepted = snapshot.rows.filter { it.accepted }.map { it.candidate }
        val overrodeDups = accepted.any {
            it.status == ParsedCandidate.Status.DUP_EXISTING ||
                it.status == ParsedCandidate.Status.DUP_BATCH
        }
        _state.update { it.copy(isCommitting = true) }
        viewModelScope.launch {
            try {
                val inserted = commitScreenshotImportUseCase(accepted)
                hasCommitted = true
                analyticsTracker.trackEvent(
                    AnalyticsEvent.ScreenshotImportCommitted(
                        inserted = inserted,
                        overrodeDups = overrodeDups
                    )
                )
                _state.update {
                    it.copy(isCommitting = false, completedInserted = inserted)
                }
                _events.trySend(ScreenshotImportEvent.Finished(inserted))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCommitting = false,
                        errorMessage = e.message ?: "Couldn't save transactions"
                    )
                }
            }
        }
    }

    private fun dismiss() {
        if (!hasCommitted) {
            analyticsTracker.trackEvent(AnalyticsEvent.ScreenshotImportCancelled)
        }
        _events.trySend(ScreenshotImportEvent.DismissedEmpty)
    }

    private fun emitParsedTelemetry(candidates: List<ParsedCandidate>) {
        val parseFailed = candidates.count { it.status == ParsedCandidate.Status.PARSE_FAILED }
        val dupsExisting = candidates.count { it.status == ParsedCandidate.Status.DUP_EXISTING }
        val dupsBatch = candidates.count { it.status == ParsedCandidate.Status.DUP_BATCH }
        val new = candidates.count { it.status == ParsedCandidate.Status.NEW }
        // "Images picked" = unique source image count. Multiple candidates
        // per image is possible in principle (not in Phase 3 which emits
        // one per image), so we dedupe by sourceImageId defensively.
        val imagesPicked = candidates.map { it.sourceImageId }.distinct().size
        analyticsTracker.trackEvent(
            AnalyticsEvent.ScreenshotImportParsed(
                imagesPicked = imagesPicked,
                parsed = new,
                dupsExisting = dupsExisting,
                dupsBatch = dupsBatch,
                parseFailed = parseFailed
            )
        )
    }
}
