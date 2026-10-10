package com.andriybobchuk.mooney.mooney.presentation.upkeep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAutoAddedPendingUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetFrequentExpenseCategoryIdsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ReviewAutoAddedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class UpkeepReviewState(
    /** All pending auto-added txs — snapshot taken on screen entry so reviewing
     *  one doesn't shift the index of the next. */
    val queue: List<Transaction> = emptyList(),
    /** Position within [queue]. When >= queue.size the terminal "all caught up"
     *  state renders. */
    val currentIndex: Int = 0,
    /** Resolved category objects for the chip grid. Order matches
     *  frequency rank (most-used first). "More…" opens the full picker. */
    val frequentCategories: List<Category> = emptyList(),
    /** Fallback full category list used by the "More…" picker. */
    val allExpenseCategories: List<Category> = emptyList(),
    /** True while initial queue + category lookup is in flight. */
    val isLoading: Boolean = true,
    /** True once the user has reached the end of the queue — triggers the
     *  "all caught up" screen and fires the completion analytics event. */
    val isCompleted: Boolean = false,
    /** True while the "More…" category picker sheet is up. */
    val isCategoryPickerOpen: Boolean = false
) {
    val currentCard: Transaction? get() = queue.getOrNull(currentIndex)
    val totalCount: Int get() = queue.size
    val progressLabel: String get() = "${(currentIndex + 1).coerceAtMost(totalCount)} of $totalCount"
}

sealed interface UpkeepReviewAction {
    /** User picked a category (or re-tapped the current one). Flips flag,
     *  swaps category if different, writes merchant→category history, advances. */
    data class PickCategory(val category: Category) : UpkeepReviewAction
    /** User opened the full-category "More…" picker. */
    data object OpenCategoryPicker : UpkeepReviewAction
    data object DismissCategoryPicker : UpkeepReviewAction
    /** User closed the review screen before reaching the end. Fires a
     *  session-completed event with the partial count. */
    data object OnDismiss : UpkeepReviewAction
}

@Suppress("LongParameterList")
class UpkeepReviewViewModel(
    private val getAutoAddedPendingUseCase: GetAutoAddedPendingUseCase,
    private val reviewAutoAddedUseCase: ReviewAutoAddedUseCase,
    private val getFrequentExpenseCategoryIdsUseCase: GetFrequentExpenseCategoryIdsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    @Suppress("unused") private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(UpkeepReviewState())
    val state: StateFlow<UpkeepReviewState> = _state.asStateFlow()

    /** Session start time for the completed-event duration bucket. */
    private val sessionStartMs: Long = Clock.System.now().toEpochMilliseconds()
    /** How many cards the user actually acted on this session (for telemetry). */
    private var cardsReviewed: Int = 0

    init {
        analyticsTracker.trackEvent(AnalyticsEvent.UpkeepOpened)
        // Snapshot the queue ONCE on entry — see comment on
        // [UpkeepReviewState.queue] for why we don't live-observe it.
        viewModelScope.launch {
            // getAllTransactions() is a Flow — grab the first emission as a
            // snapshot. Combined with frequent-categories so initial state
            // has both halves ready, no mid-render flicker.
            combine(
                getAutoAddedPendingUseCase(),
                getFrequentExpenseCategoryIdsUseCase()
            ) { pending, frequentIds ->
                val frequentCategories = frequentIds.mapNotNull { getCategoriesUseCase(it) }
                val allExpense = getCategoriesUseCase().filter {
                    it.type == com.andriybobchuk.mooney.mooney.domain.CategoryType.EXPENSE &&
                        it.parent != null // only subcategories (same filter as Add Transaction sheet)
                }
                Triple(pending, frequentCategories, allExpense)
            }
                .onEach { (pending, frequentCategories, allExpense) ->
                    _state.update {
                        // Only seed the queue on the FIRST emission — subsequent
                        // emissions would shift indices as the user reviews.
                        val seededQueue = if (it.queue.isEmpty() && !it.isCompleted) {
                            pending
                        } else {
                            it.queue
                        }
                        it.copy(
                            queue = seededQueue,
                            frequentCategories = frequentCategories,
                            allExpenseCategories = allExpense,
                            isLoading = false,
                            // If the queue was empty from the start, flip
                            // straight to the completion state so the user
                            // sees "all caught up" instead of a blank card.
                            isCompleted = seededQueue.isEmpty()
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onAction(action: UpkeepReviewAction) {
        when (action) {
            is UpkeepReviewAction.PickCategory -> handlePickCategory(action.category)
            UpkeepReviewAction.OpenCategoryPicker ->
                _state.update { it.copy(isCategoryPickerOpen = true) }
            UpkeepReviewAction.DismissCategoryPicker ->
                _state.update { it.copy(isCategoryPickerOpen = false) }
            UpkeepReviewAction.OnDismiss -> emitSessionCompletedEvent()
        }
    }

    private fun handlePickCategory(category: Category) {
        val current = _state.value.currentCard ?: return
        val isConfirm = category.id == current.subcategory.id
        viewModelScope.launch {
            reviewAutoAddedUseCase(current, category.id)
            cardsReviewed += 1
            analyticsTracker.trackEvent(
                AnalyticsEvent.UpkeepCardAction(
                    action = if (isConfirm) "confirm" else "change",
                    merchantMatched = !current.description.isNullOrBlank()
                )
            )
            advanceToNextCard()
        }
    }

    private fun advanceToNextCard() {
        _state.update { s ->
            val next = s.currentIndex + 1
            if (next >= s.queue.size) {
                // Completion — fire telemetry here (not in onDismiss) so we
                // distinguish "reached the end" from "bailed early".
                analyticsTracker.trackEvent(
                    AnalyticsEvent.UpkeepSessionCompleted(
                        cardsReviewed = cardsReviewed,
                        reachedEnd = true
                    )
                )
                s.copy(currentIndex = next, isCompleted = true, isCategoryPickerOpen = false)
            } else {
                s.copy(currentIndex = next, isCategoryPickerOpen = false)
            }
        }
    }

    /** Fires when the user leaves the screen without finishing. Distinct from
     *  the completion event (fired in [advanceToNextCard]) so we can measure
     *  bail rate. */
    private fun emitSessionCompletedEvent() {
        if (_state.value.isCompleted) return // already fired on completion
        analyticsTracker.trackEvent(
            AnalyticsEvent.UpkeepSessionCompleted(
                cardsReviewed = cardsReviewed,
                reachedEnd = false
            )
        )
    }
}
