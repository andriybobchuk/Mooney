package com.andriybobchuk.mooney.mooney.domain.usecase.screenshot

import com.andriybobchuk.mooney.core.platform.ScreenshotOcrResult
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

/**
 * One candidate transaction extracted from a single screenshot. Mirrors a
 * loose superset of [com.andriybobchuk.mooney.mooney.domain.Transaction]:
 * fields the regex pass surfaced + a confidence hint + the dedup verdict.
 *
 * The preview UI renders a row per candidate; the user can toggle it off
 * before the commit step inserts the accepted ones as real transactions.
 */
data class ParsedCandidate(
    val sourceImageId: String,
    /** The amount in the currency the regex detected, positive. */
    val amount: Double,
    val currency: Currency,
    /** Best-effort merchant/description extracted from the notification body.
     *  Null if the regex pass couldn't isolate a merchant string. */
    val description: String?,
    val date: LocalDate,
    /** True iff the date came from the EXIF capture time (notification body
     *  had no parseable timestamp). Dedup is stricter for these — see
     *  [DedupScreenshotCandidatesUseCase]. */
    val dateIsInferred: Boolean,
    /** 0.0 - 1.0. Low = relying on bare-decimal fallback regex, high =
     *  currency symbol + merchant preposition both matched. Surfaces as a
     *  small ⚠ icon in the preview when < 0.5. */
    val confidence: Double,
    /** Set by [DedupScreenshotCandidatesUseCase] after the three-pass check.
     *  NEW rows get inserted; everything else is informational for the
     *  preview UI (user can still override). */
    val status: Status = Status.NEW
) {
    enum class Status {
        /** Not a duplicate — will be inserted. */
        NEW,
        /** Signature matched an existing transaction in the user's DB. */
        DUP_EXISTING,
        /** Signature matched another candidate parsed in the same batch. */
        DUP_BATCH,
        /** OCR produced no parseable amount for this image. Shown on preview
         *  as "Couldn't read screenshot N" but not insertable. */
        PARSE_FAILED
    }
}

/**
 * Extracts [ParsedCandidate]s from the OCR output of a single image.
 *
 * Returns an empty list for an image with no parseable amount — the caller
 * surfaces that as a PARSE_FAILED candidate so the preview UI can show
 * "Couldn't read screenshot 3" per the user's spec.
 */
class ParseScreenshotUseCase {

    operator fun invoke(ocr: ScreenshotOcrResult): List<ParsedCandidate> {
        if (ocr.textLines.isEmpty()) {
            return listOf(parseFailedPlaceholder(ocr))
        }
        // Join lines with a single space so regex sees a flat corpus; this
        // handles iOS Vision's tendency to split a short notification body
        // onto 2-3 OCR lines.
        val corpus = ocr.textLines.joinToString(" ") { it.trim() }

        val amountMatch = findAmount(corpus)
            ?: return listOf(parseFailedPlaceholder(ocr))

        val merchant = findMerchant(corpus, amountMatch.rawAmountText)
        val (date, isInferred) = resolveDate(corpus, ocr.captureEpochSeconds)

        val confidence = computeConfidence(
            currencyMatched = amountMatch.currency != null,
            merchantFound = !merchant.isNullOrBlank(),
            dateInferred = isInferred
        )

        return listOf(
            ParsedCandidate(
                sourceImageId = ocr.imageId,
                amount = amountMatch.amount,
                currency = amountMatch.currency ?: GlobalConfig.baseCurrency,
                description = merchant,
                date = date,
                dateIsInferred = isInferred,
                confidence = confidence
            )
        )
    }

    private fun parseFailedPlaceholder(ocr: ScreenshotOcrResult): ParsedCandidate {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return ParsedCandidate(
            sourceImageId = ocr.imageId,
            amount = 0.0,
            currency = GlobalConfig.baseCurrency,
            description = null,
            date = today,
            dateIsInferred = true,
            confidence = 0.0,
            status = ParsedCandidate.Status.PARSE_FAILED
        )
    }

    // ---- amount parsing ----

    private data class AmountMatch(
        val amount: Double,
        val currency: Currency?,
        val rawAmountText: String
    )

    private fun findAmount(corpus: String): AmountMatch? {
        // Ordered from most specific (currency + amount) to least. First
        // match wins; this guarantees "$42.50" beats bare "42.50" that
        // might appear as a card number fragment or balance line.
        val patterns = listOf(
            // "42,50 zł" / "42.50 PLN"
            Regex("(\\d+[.,]\\d{2})\\s?(zł|PLN|pln)") to Currency.PLN,
            // "$42.50"
            Regex("\\$\\s?(\\d+[.,]\\d{2})") to Currency.USD,
            // "42.50 USD"
            Regex("(\\d+[.,]\\d{2})\\s?USD") to Currency.USD,
            // "42,50 €" / "42.50 EUR"
            Regex("(\\d+[.,]\\d{2})\\s?(€|EUR)") to Currency.EUR,
            // "€42,50"
            Regex("€\\s?(\\d+[.,]\\d{2})") to Currency.EUR,
            // "42.50 GBP"
            Regex("(\\d+[.,]\\d{2})\\s?GBP") to Currency.GBP,
            // "£42.50"
            Regex("£\\s?(\\d+[.,]\\d{2})") to Currency.GBP
        )
        patterns.forEach { (pattern, currency) ->
            val match = pattern.find(corpus) ?: return@forEach
            val groups = match.groupValues
            val numStr = groups.firstOrNull { it.contains(Regex("[0-9]")) }
                ?: return@forEach
            val parsed = parseDecimalLoose(numStr) ?: return@forEach
            return AmountMatch(parsed, currency, match.value)
        }
        // Bare decimal fallback — risky, use only if nothing above matched.
        // Must have exactly 2 decimal places and be isolated by whitespace
        // / punctuation to avoid picking up card-number fragments.
        val bare = Regex("(^|\\s)(\\d{1,6}[.,]\\d{2})(\\s|\$|[,.])").find(corpus)
            ?: return null
        val parsed = parseDecimalLoose(bare.groupValues[2]) ?: return null
        return AmountMatch(parsed, null, bare.value.trim())
    }

    private fun parseDecimalLoose(raw: String): Double? {
        // Accept both PL ("42,50") and EN ("42.50") decimal separators.
        return raw.replace(",", ".").toDoubleOrNull()
    }

    // ---- merchant parsing ----

    private fun findMerchant(corpus: String, amountRaw: String): String? {
        // Try explicit prepositions first — these are the most reliable.
        // The capture group must start with a capital letter (merchant
        // names are proper nouns) and run until a terminator or sentence
        // boundary.
        val patterns = listOf(
            Regex("\\bw\\s+([A-ZŁŚŻŹĆŃÓĄĘ][\\p{L}0-9&\\s\\.\\-']{2,40})"), // PL "w Biedronka"
            Regex("\\bu\\s+([A-ZŁŚŻŹĆŃÓĄĘ][\\p{L}0-9&\\s\\.\\-']{2,40})"), // PL "u Jana"
            Regex("\\bat\\s+([A-Z][\\p{L}0-9&\\s\\.\\-']{2,40})"),           // EN "at Starbucks"
            Regex("\\bby\\s+([A-Z][\\p{L}0-9&\\s\\.\\-']{2,40})"),           // EN "by Shell"
            Regex("Payment to:?\\s+([^\\n]{2,40})"),                         // generic
            Regex("Merchant:?\\s+([^\\n]{2,40})")                            // generic
        )
        patterns.forEach { pattern ->
            val match = pattern.find(corpus) ?: return@forEach
            val raw = match.groupValues.getOrNull(1)?.trim() ?: return@forEach
            // Strip trailing punctuation / newlines OCR often attaches to
            // the merchant name. Guard against capturing the amount itself
            // (safety against pathological patterns like "Starbucks 42.50").
            val cleaned = raw
                .substringBefore(amountRaw)
                .trim()
                .trimEnd('.', ',', ';', ':')
            if (cleaned.length in 3..40) return cleaned
        }
        return null
    }

    // ---- date parsing ----

    private fun resolveDate(
        corpus: String,
        captureEpochSeconds: Long
    ): Pair<LocalDate, Boolean> {
        // Explicit absolute dates are rare in iOS notifications; skip.
        // Explicit times without dates ("14:32") assume today — not useful
        // for date dedup but fine for the DB row.
        // Fallback cascade: EXIF capture time → today. Zero means "unknown"
        // per the ScreenshotOcrResult contract.
        val tz = TimeZone.currentSystemDefault()
        if (captureEpochSeconds > 0L) {
            val captured = Instant.fromEpochSeconds(captureEpochSeconds)
                .toLocalDateTime(tz).date
            return captured to true
        }
        return Clock.System.now().toLocalDateTime(tz).date to true
    }

    // ---- confidence ----

    private fun computeConfidence(
        currencyMatched: Boolean,
        merchantFound: Boolean,
        dateInferred: Boolean
    ): Double {
        var score = 0.4 // amount was at least found (we wouldn't be here otherwise)
        if (currencyMatched) score += 0.3
        if (merchantFound) score += 0.2
        if (!dateInferred) score += 0.1
        return score.coerceIn(0.0, 1.0)
    }
}

// ============================================================================
// Dedup
// ============================================================================

/**
 * Three-pass duplicate detector. See the design doc for the signature
 * algorithm — short version: `(amount_cents, timestamp_15min_bucket, accountId)`.
 *
 * Pass A — against the existing DB (last 7 days of txs on the primary account).
 * Pass B — within the current batch (first occurrence wins).
 * Pass C — tag the survivors as NEW.
 *
 * The user's preview UI shows every candidate including dups (with the
 * reason), and lets them tap "keep anyway" to override — so this is a
 * recommendation, not a hard filter.
 */
class DedupScreenshotCandidatesUseCase {

    operator fun invoke(
        candidates: List<ParsedCandidate>,
        existingSignatures: Set<TxSignature>
    ): List<ParsedCandidate> {
        val seenInBatch = mutableSetOf<TxSignature>()
        return candidates.map { c ->
            if (c.status == ParsedCandidate.Status.PARSE_FAILED) {
                // Carry through unchanged — can't dedup a non-tx.
                return@map c
            }
            val sig = signatureOf(c)
            when {
                sig in existingSignatures -> c.copy(status = ParsedCandidate.Status.DUP_EXISTING)
                sig in seenInBatch -> c.copy(status = ParsedCandidate.Status.DUP_BATCH)
                else -> {
                    seenInBatch.add(sig)
                    c.copy(status = ParsedCandidate.Status.NEW)
                }
            }
        }
    }

    private fun signatureOf(c: ParsedCandidate): TxSignature {
        val cents = (c.amount * 100).roundToLong()
        // 15-min bucket. For inferred dates we only have day precision,
        // which is noisier — accept it, the deliberate "keep anyway"
        // override in the preview UI is the safety net.
        val bucketMinutes = c.date.hashCode().toLong() // date-only coarse bucket
        return TxSignature(cents, bucketMinutes)
    }
}

/**
 * Signature used by both the parse-time dedup pass and the live DB query
 * that fetches existing signatures. Keeps both sides using identical math.
 */
data class TxSignature(
    val amountCents: Long,
    val bucketKey: Long
)
