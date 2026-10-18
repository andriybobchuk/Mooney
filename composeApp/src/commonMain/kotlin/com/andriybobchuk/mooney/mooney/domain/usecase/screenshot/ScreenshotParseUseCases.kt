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
/**
 * Which screenshot layout the parser should expect. The user explicitly
 * requested two separate scan buttons (one per format) rather than a smart
 * "auto-detect" fallback — mixing both strategies produced false merchants
 * on real screenshots when the heuristic guessed wrong. Each mode is now
 * SURGICALLY focused on exactly one layout; nothing falls through.
 */
enum class ParseMode {
    /**
     * iOS notification-centre cards: `"Nowa transakcja kartą ... w wysokości
     * X PLN w <MERCHANT>."` — merchant lives AFTER the amount, introduced
     * by a preposition keyword (w/u/at/by). Dates come from EXIF (not in
     * the text itself).
     */
    NOTIFICATION,

    /**
     * In-bank-app history list: stacked rows of `<MERCHANT> Card operation
     * -X,XX PLN` grouped under `9 October | The day before yesterday`
     * headers. Merchant lives BEFORE the amount, terminated by the
     * "Card operation" boilerplate; dates come from the headers.
     */
    BANK_HISTORY
}

class ParseScreenshotUseCase {

    operator fun invoke(
        ocr: ScreenshotOcrResult,
        mode: ParseMode = ParseMode.NOTIFICATION
    ): List<ParsedCandidate> {
        if (ocr.textLines.isEmpty()) {
            return listOf(parseFailedPlaceholder(ocr))
        }
        val cleanedLines = ocr.textLines.map { scrubLine(it) }
        val corpus = cleanedLines.joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")

        val amountMatches = findAllAmounts(corpus, mode)
        if (amountMatches.isEmpty()) {
            return listOf(parseFailedPlaceholder(ocr))
        }

        val (fallbackDate, isFallbackInferred) = resolveDate(corpus, ocr.captureEpochSeconds)
        // Only BANK_HISTORY mode scans for date headers — notifications
        // have no in-text dates, and running the header regex on them
        // wastes cycles and risks a false positive on merchant words that
        // happen to look like "9 October" (rare but possible on long
        // transaction notes).
        val dateHeaders = if (mode == ParseMode.BANK_HISTORY) {
            parseDateHeaders(corpus)
        } else {
            emptyList()
        }

        val candidates = amountMatches.mapIndexed { index, amount ->
            val merchantRangeStart = amountMatches.getOrNull(index - 1)?.endIndex ?: 0
            val merchantRangeEnd = amountMatches.getOrNull(index + 1)?.startIndex
                ?: corpus.length
            val merchant = when (mode) {
                ParseMode.NOTIFICATION ->
                    findMerchantRightOfAmount(corpus, amount, merchantRangeEnd)
                        ?: findMerchantLastDitch(corpus, amount, merchantRangeStart, merchantRangeEnd)
                ParseMode.BANK_HISTORY ->
                    findMerchantLeftOfAmount(corpus, amount, merchantRangeStart)
                        ?: findMerchantLastDitch(corpus, amount, merchantRangeStart, merchantRangeEnd)
            }
            val headerDate = dateHeaders
                .filter { it.first <= amount.startIndex }
                .maxByOrNull { it.first }
                ?.second
            val resolvedDate = headerDate ?: fallbackDate
            val isInferred = headerDate == null && isFallbackInferred
            val confidence = computeConfidence(
                currencyMatched = amount.currency != null,
                merchantFound = !merchant.isNullOrBlank(),
                dateInferred = isInferred,
                matchedBankPattern = amount.strong
            )
            ParsedCandidate(
                sourceImageId = "${ocr.imageId}#$index",
                amount = amount.amount,
                currency = amount.currency ?: GlobalConfig.baseCurrency,
                description = merchant,
                date = resolvedDate,
                dateIsInferred = isInferred,
                confidence = confidence
            )
        }
        // In-image dedup: a notification body sometimes repeats the amount
        // in title + body. Use amount + (lowercased merchant) as the identity
        // key here — this is DIFFERENT from the cross-image dedup pass that
        // runs against existing DB rows.
        return candidates.distinctBy { it.amount to (it.description?.lowercase() ?: "") }
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

    /**
     * Removes OCR noise that confuses downstream regex:
     * - Masked card-number blocks ("5575 **** **** 8308" or variants where
     *   `*` was OCR'd as digits/letters) — the leading "card XXXX ****"
     *   fragment gets eaten so it can't match an amount or merchant.
     * - Bare quasi-card runs like "5575 1111 2222 8308" (same shape, pure
     *   digits) — a conservative guard: 4-group blocks of 4 digits/stars.
     */
    private fun scrubLine(line: String): String {
        // "5575 **** **** 8308", "5575 1111 1111 8308", "5575**** ****8308"
        val cardBlockA = Regex("\\d{4}\\s*[*\\dxX]{3,4}\\s*[*\\dxX]{3,4}\\s*\\d{4}")
        return line.replace(cardBlockA, " ")
    }

    // ---- amount parsing ----

    /**
     * `strong = true` means the amount came from a known bank-notification
     * template ("w wysokości X PLN", "amount of X", etc.) which earns higher
     * confidence than a bare currency+number co-occurrence.
     */
    private data class AmountMatch(
        val amount: Double,
        val currency: Currency?,
        val rawAmountText: String,
        /** Character index where the matched substring STARTS in `corpus`.
         *  Used by multi-transaction extraction to bound the merchant search
         *  of the previous candidate (merchant ends where the next amount
         *  begins). */
        val startIndex: Int,
        /** Character index where the matched substring ENDS (exclusive) —
         *  merchant extraction for THIS candidate searches from here. */
        val endIndex: Int,
        val strong: Boolean
    )

    /**
     * Find EVERY amount occurrence in the corpus, sorted by position, then
     * dedup overlaps so a single site isn't reported twice (e.g. the strong
     * "w wysokości 42.00 PLN" and the generic "42.00 PLN" both match the
     * same characters — we keep the strong one).
     *
     * Returned list is in LEFT-TO-RIGHT order, so the caller can bound
     * merchant extraction for candidate N by the start of candidate N+1.
     */
    private fun findAllAmounts(corpus: String, mode: ParseMode): List<AmountMatch> {
        val all = mutableListOf<AmountMatch>()
        // Strong "w wysokości X PLN" / "amount of X" patterns are a
        // NOTIFICATION-only signal — bank-history rows never carry that
        // phrasing. Skipping it in BANK_HISTORY mode prevents the strong
        // regex from false-matching if a bank app ever embeds a chatty
        // tooltip in its list cells.
        if (mode == ParseMode.NOTIFICATION) {
            all += findAllStrongBankPatterns(corpus)
        }
        all += findAllCurrencyMarkerPatterns(corpus, mode)

        // Bare-decimal fallback is a NOTIFICATION-mode-only safety net —
        // only runs if nothing stronger matched. Running it in BANK_HISTORY
        // mode is actively harmful because the row's `-60 000,00 PLN`
        // income line (which we WANT to filter out due to no sign) still
        // leaves a `0,00` sub-match that the bare-decimal regex would
        // happily pick up, bypassing the sign gate.
        if (mode == ParseMode.NOTIFICATION && all.isEmpty()) {
            all += findAllBareDecimals(corpus)
        }
        // Dedup overlapping matches: a strong pattern anchor like
        // "w wysokości 42.00 PLN" shares the "42.00 PLN" substring with the
        // generic Pass-2 currency marker. Prefer strong (more info). An
        // overlap exists when ranges intersect.
        return deduplicateOverlapping(all)
            .sortedBy { it.startIndex }
    }

    private fun deduplicateOverlapping(matches: List<AmountMatch>): List<AmountMatch> {
        // Sort strong-first so when we drop later overlaps, the kept entry
        // is always the stronger one.
        val sorted = matches.sortedWith(
            compareByDescending<AmountMatch> { it.strong }.thenBy { it.startIndex }
        )
        val kept = mutableListOf<AmountMatch>()
        for (m in sorted) {
            val overlaps = kept.any { k ->
                m.startIndex < k.endIndex && k.startIndex < m.endIndex
            }
            if (!overlaps) kept += m
        }
        return kept
    }

    private fun findAllStrongBankPatterns(corpus: String): List<AmountMatch> {
        val patterns: List<Regex> = listOf(
            Regex("w\\s+wysokości\\s+($amountBodyPattern)\\s*(zł|PLN|pln|EUR|USD|GBP)?", RegexOption.IGNORE_CASE),
            Regex(
                "(?:amount|payment|charged|spent|debited|paid|charge)\\s*(?:of|was)?\\s*" +
                    "(?:[\$£€])?($amountBodyPattern)\\s*(zł|PLN|pln|EUR|USD|GBP|€|\\\$|£)?",
                RegexOption.IGNORE_CASE
            )
        )
        return patterns.flatMap { pattern ->
            pattern.findAll(corpus).mapNotNull { match ->
                val amountStr = match.groupValues.getOrNull(1) ?: return@mapNotNull null
                val parsed = parseDecimalLoose(amountStr) ?: return@mapNotNull null
                val currency = resolveCurrencyToken(match.groupValues.lastOrNull())
                AmountMatch(
                    amount = parsed,
                    currency = currency,
                    rawAmountText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    strong = true
                )
            }.toList()
        }
    }

    /**
     * Decimal numeric body with optional space-grouped thousands AND
     * optional whitespace around the decimal separator.
     * - Matches `60 000,00`, `60000,00`, `20,95`, `0,02`, `4 374,37`.
     * - ALSO matches `20 ,95`, `20, 95`, `20 , 95` — Vision splits bold
     *   integer from thin decimal/currency due to font-weight changes in
     *   the bank's display, producing variable whitespace around the comma.
     * - Does NOT match a bare `12345` or `123.4` (requires exactly 2 decimals).
     */
    private val amountBodyPattern = "(?:\\d{1,3}(?:\\s\\d{3})+|\\d+)\\s*[.,]\\s*\\d{2}"

    @Suppress("ReturnCount")
    private fun findAllCurrencyMarkerPatterns(corpus: String, mode: ParseMode): List<AmountMatch> {
        // BANK_HISTORY mode: ONLY accept PLN with a leading `-` (minus) sign.
        // User explicitly asked to ignore income rows (positive amounts) and
        // non-PLN currencies — those are typically transfers/exchange ops
        // that shouldn't become auto-added spending transactions.
        // NOTIFICATION mode: accept every currency, sign optional.
        val patterns: List<Pair<Regex, Currency>> = if (mode == ParseMode.BANK_HISTORY) {
            listOf(
                // Mandatory `-` or `−` (U+2212) prefix, PLN or zł suffix.
                // The ASCII hyphen is overwhelmingly what iOS Vision OCRs
                // from the bank app's row markup, but we accept the Unicode
                // minus too in case the glyph varies.
                Regex("[-−]\\s*($amountBodyPattern)\\s?(PLN|pln|zł)") to Currency.PLN
            )
        } else {
            listOf(
                Regex("($amountBodyPattern)\\s?(zł|PLN|pln)") to Currency.PLN,
                Regex("\\\$\\s?($amountBodyPattern)") to Currency.USD,
                Regex("($amountBodyPattern)\\s?USD") to Currency.USD,
                Regex("($amountBodyPattern)\\s?(€|EUR)") to Currency.EUR,
                Regex("€\\s?($amountBodyPattern)") to Currency.EUR,
                Regex("($amountBodyPattern)\\s?GBP") to Currency.GBP,
                Regex("£\\s?($amountBodyPattern)") to Currency.GBP
            )
        }
        return patterns.flatMap { (pattern, currency) ->
            pattern.findAll(corpus).mapNotNull { match ->
                val numStr = match.groupValues.getOrNull(1) ?: return@mapNotNull null
                val parsed = parseDecimalLoose(numStr) ?: return@mapNotNull null
                AmountMatch(
                    amount = parsed,
                    currency = currency,
                    rawAmountText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    strong = false
                )
            }.toList()
        }
    }

    private fun findAllBareDecimals(corpus: String): List<AmountMatch> {
        return Regex("(^|\\s)(\\d{1,6}[.,]\\d{2})(\\s|\$|[,.])")
            .findAll(corpus)
            .mapNotNull { match ->
                val parsed = parseDecimalLoose(match.groupValues[2]) ?: return@mapNotNull null
                AmountMatch(
                    amount = parsed,
                    currency = null,
                    rawAmountText = match.value.trim(),
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    strong = false
                )
            }.toList()
    }

    private fun resolveCurrencyToken(token: String?): Currency? {
        val t = token?.trim()?.lowercase() ?: return null
        return when (t) {
            "pln", "zł", "zl" -> Currency.PLN
            "usd", "$" -> Currency.USD
            "eur", "€" -> Currency.EUR
            "gbp", "£" -> Currency.GBP
            else -> null
        }
    }

    private fun parseDecimalLoose(raw: String): Double? {
        // Accept both PL ("42,50") and EN ("42.50") decimal separators, and
        // strip any space-grouped thousands separator ("60 000,00" → "60000.00").
        return raw
            .replace(" ", "")
            .replace(" ", "") // non-breaking space variant
            .replace(",", ".")
            .toDoubleOrNull()
    }

    // ---- merchant parsing ----

    /**
     * Position-aware merchant extractor. Searches ONLY the substring that
     * comes AFTER the amount in the corpus — this means:
     * - The leading "w wysokości" (Polish for "in the amount of") never
     *   gets mis-captured as "w <MERCHANT>".
     * - Merchants that start with digits ("021 KRAKOW BONARKA") work: we
     *   drop the "must start with uppercase letter" constraint entirely
     *   because the keyword prefix ("w ", "at ", "by ") already anchors us.
     * - Special chars in merchant names ("/", ".", "&") pass through
     *   because we grab everything up to a terminator, not a strict class.
     */
    /**
     * Two merchant-extraction strategies, tried in order:
     *
     * 1. **Bank-history format** (merchant is BEFORE the amount, terminated
     *    by "Card operation" / "Operacja kartą" / similar bank-list
     *    boilerplate). Example: `"PEPCO 110340 KRAKOW 23, K… Card operation
     *    -227,90 PLN"` — merchant lives between the previous amount and the
     *    "Card operation" string that precedes this amount.
     * 2. **Notification format** (merchant is AFTER the amount, preceded by
     *    a preposition keyword). Example: `"w wysokości 164.00 PLN w Nova
     *    Sushi"` — the historic strategy.
     */
    /**
     * Last-ditch merchant extraction. Priority: ALWAYS return something
     * non-null if there's any text at all around the amount. The user
     * explicitly asked that scanned txs always carry a description — "no
     * way to identify the amount without going back to the bank" — so a
     * noisy best-guess merchant beats no label at all.
     *
     * Strategy:
     *   1. Try the LEFT side with full scrubbing (date headers, bank
     *      subtitles, bank UI chrome, other amount tokens, currency words).
     *   2. If left scrub produces something valid, use it.
     *   3. Else try the RIGHT side with the same scrubbing (notification
     *      format puts merchant on the right).
     *   4. Else take WHATEVER non-whitespace alphanumeric word of 2+ chars
     *      sits closest to the amount. Even "Am" is better than null.
     *   5. Only return null if there is literally no printable text in
     *      either direction.
     */
    @Suppress("ReturnCount")
    private fun findMerchantLastDitch(
        corpus: String,
        amount: AmountMatch,
        rangeStart: Int,
        rangeEnd: Int
    ): String? {
        val leftStart = rangeStart.coerceAtMost(corpus.length)
        val leftEnd = amount.startIndex.coerceIn(leftStart, corpus.length)
        val rightStart = amount.endIndex.coerceAtMost(corpus.length)
        val rightEnd = rangeEnd.coerceIn(rightStart, corpus.length)
        val leftText = corpus.substring(leftStart, leftEnd)
        val rightText = corpus.substring(rightStart, rightEnd)

        // Pass 1: scrub left-side text, prefer it (bank-history layout).
        scrubForLastDitch(leftText).takeIf { isValidMerchant(it) }?.let { return it }
        // Pass 2: scrub right-side text (notification layout fallback).
        scrubForLastDitch(rightText).takeIf { isValidMerchant(it) }?.let { return it }
        // Pass 3: pull ANY alphanumeric token of 2+ chars from either side.
        // Prefer the longer alphanumeric run so we get the full merchant
        // name even if the scrub stripped surrounding noise too aggressively.
        val anyWordLeft = extractLongestWord(leftText)
        val anyWordRight = extractLongestWord(rightText)
        val best = listOfNotNull(anyWordLeft, anyWordRight).maxByOrNull { it.length }
        return best
    }

    /**
     * Finds the longest alphanumeric-ish token (letters, digits, &, ., -, *,
     * ', spaces) in the input. Minimum 2 chars. Used as the absolute last
     * resort when scrubbed extraction produces nothing valid.
     */
    private fun extractLongestWord(s: String): String? {
        if (s.isBlank()) return null
        return Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}&.,'/*\\- ]{1,58}")
            .findAll(s)
            .map { it.value.trim().trimEnd('.', ',', ';', ':', '-', '*', '…', ' ') }
            .filter { it.length >= 2 }
            .maxByOrNull { it.length }
    }

    private fun scrubForLastDitch(raw: String): String {
        var text = raw
        // Drop date headers + their relative suffix.
        text = stripLeadingDateHeader(text)
        // Drop bank-list boilerplate words wherever they appear.
        text = text.replace(
            Regex(
                "(?i)\\b(" +
                    "Card\\s+operation|Operacja\\s+kart[aą]|Transakcja\\s+kartowa|Płatność\\s+kart[aą]|" +
                    "Own\\s+transfer|Przelew\\s+własny|" +
                    "Domestic\\s+transfer|Przelew\\s+krajowy|" +
                    "Exchange\\s+office\\s+operation|Operacja\\s+kantorowa|" +
                    "Odsetki[\\s\\-]+Kapitalizacja\\s+odsetek|Podatek\\s+od\\s+odsetek|" +
                    "Nowa\\s+transakcja|Transakcja|Transaction|You\\s+paid|Payment\\s+of|" +
                    "w\\s+wysokości|wysokości" +
                    ")\\b"
            ),
            " "
        )
        // Drop ANY other amount tokens (other transaction rows in the slice).
        text = text.replace(
            Regex("[-−+]?(?:\\d{1,3}(?:\\s\\d{3})+|\\d+)[.,]\\d{2}\\s*(?:PLN|EUR|USD|GBP|zł|\\$|€|£)?"),
            " "
        )
        // Drop known currency-only tokens.
        text = text.replace(Regex("(?i)\\b(PLN|EUR|USD|GBP|zł)\\b"), " ")
        return text
            .trim()
            .trimEnd('.', ',', ';', ':', ' ', '\t', '-', '*', '…')
            .replace(Regex("\\s{2,}"), " ")
    }

    /**
     * Bank-history list format merchant extractor.
     *
     * iOS Vision OCR reads top-to-bottom, left-to-right. A real bank-list
     * row looks like:
     *
     *     MERCHANT, CITY, PL        -X,XX PLN      (visually one line, amount right-aligned)
     *     Card operation                           (bottom-left of the row)
     *
     * Vision could serialize this as ANY of:
     *   A. `[merchant, amount, "Card operation"]`     — most common
     *   B. `[merchant, "Card operation", amount]`     — my old test assumption
     *   C. `[merchant_and_amount_block, "Card operation"]` — single-block capture
     *
     * The ORIGINAL implementation assumed (B) and silently failed on (A),
     * which is the dominant case in real screenshots — the user reported
     * "none bruv" descriptions for every bank-mode scan.
     *
     * New strategy — works for all three orderings:
     *   1. Preceding region = everything between the previous amount's end
     *      and the current amount's start.
     *   2. SCRUB every known subtitle terminator ("Card operation" and
     *      friends) from that region.
     *   3. Scrub bank-app UI chrome ("History", "VeloBank S.A.", standalone
     *      ellipsis, back-arrow glyphs).
     *   4. Strip leading date header.
     *   5. Whatever's left is the merchant text.
     *
     * This naturally handles (A) because "Card operation" of the PREVIOUS
     * row appears in the current amount's preceding region, and we remove
     * it. (B) also works because the terminator we remove sits between
     * the merchant and the amount. (C) works because the amount splits
     * cleanly from the merchant text to its left.
     */
    @Suppress("ReturnCount")
    private fun findMerchantLeftOfAmount(
        corpus: String,
        amount: AmountMatch,
        rangeStart: Int
    ): String? {
        val startIdx = rangeStart.coerceAtMost(corpus.length)
        val endIdx = amount.startIndex.coerceIn(startIdx, corpus.length)
        val preceding = corpus.substring(startIdx, endIdx)
            .ifEmpty { return null }

        val cleaned = preceding
            // 1. Remove every bank-subtitle terminator — wherever it appears
            //    in the preceding region, it does NOT belong to the merchant.
            .replace(bankSubtitleRegex, " ")
            // 2. Remove bank-app UI chrome that leaks into OCR output.
            .replace(bankUiChromeRegex, " ")
            // 3. Strip leading date header + any "| Yesterday" / "| 3 days"
            //    annotation that follows it.
            .let { stripLeadingDateHeader(it) }
            // 4. Normalize whitespace and strip trailing punctuation left
            //    behind by the terminator-removal pass.
            .trim()
            .trimEnd('.', ',', ';', ':', ' ', '\t', '-', '*', '…')
            .replace(Regex("\\s{2,}"), " ")

        return cleaned.takeIf { isValidMerchant(it) }
    }

    /**
     * Row-type subtitles printed BELOW the merchant name on every bank-list
     * card. Shared between the main extractor + the last-ditch fallback.
     * Non-card types (transfers, interest, tax) are included so if the sign
     * gate ever lets one through, the merchant text still reads cleanly.
     */
    private val bankSubtitleRegex = Regex(
        "(?i)\\b(" +
            // Card payment rows (expense)
            "Card\\s+operation|Operacja\\s+kart[aą]|Transakcja\\s+kartowa|Płatność\\s+kart[aą]|" +
            // Transfer rows
            "Own\\s+transfer|Przelew\\s+własny|" +
            "Domestic\\s+transfer|Przelew\\s+krajowy|" +
            "Exchange\\s+office\\s+operation|Operacja\\s+kantorowa|" +
            // Interest / tax rows
            "Odsetki[\\s\\-]+Kapitalizacja\\s+odsetek|" +
            "Podatek\\s+od\\s+odsetek" +
            ")\\b"
    )

    /**
     * Bank-app UI chrome that leaks into OCR output of a history screen —
     * screen title, bank name on interest rows, standalone pagination
     * glyphs. Stripped so they don't pollute merchant text when they
     * appear at the top of the first row's preceding region.
     */
    private val bankUiChromeRegex = Regex(
        "(?i)\\b(History|Historia|VeloBank\\s+S\\.A\\.?)\\b"
    )

    /**
     * Notification format: `[prefix] [amount] [preposition] [merchant] [.]`.
     * This is the pre-bank-history strategy — merchant walks FORWARD from
     * the amount end, consumed by a word-boundary preposition keyword.
     */
    @Suppress("ReturnCount")
    private fun findMerchantRightOfAmount(
        corpus: String,
        amount: AmountMatch,
        rangeEnd: Int
    ): String? {
        // IMPORTANT: don't `.trim()` the tail — the separator keywords below
        // rely on a leading space ("w LEWIATAN" vs. " w LEWIATAN"), and
        // trimming eats exactly that signal. Trim happens on the final
        // cleaned merchant string instead.
        // Bounded by `rangeEnd` so when multiple notifications are stacked
        // in one screenshot, the merchant for tx N doesn't swallow the
        // boilerplate ("Nowa transakcja kartą...") of tx N+1.
        val startIdx = amount.endIndex.coerceAtMost(corpus.length)
        val endIdx = rangeEnd.coerceIn(startIdx, corpus.length)
        val tail = corpus.substring(startIdx, endIdx)
            .ifEmpty { return null }

        // Word-boundary matcher — matches the keyword as a whole token
        // regardless of surrounding whitespace. Covers Polish prepositions
        // ("w"/"u"/"do"/"od"), English ("at"/"by"/"to"/"from"), and
        // generic separators ("@", ":", "-").
        val sepPattern = Regex(
            "\\s*(?:\\b(?:w|u|do|od|at|by|to|from)\\b|@|:|-|–)\\s+",
            RegexOption.IGNORE_CASE
        )
        val sepMatch = sepPattern.find(tail)
        val merchantRaw = if (sepMatch != null) {
            tail.substring(sepMatch.range.last + 1)
        } else {
            // No separator — some banks drop the preposition entirely and
            // write "42.50 PLN STARBUCKS KRAKOW". Take the tail wholesale.
            tail
        }

        val cleaned = merchantRaw
            // Cut off at a sentence terminator — notifications often have
            // trailing ".", ellipses, or further bank boilerplate.
            .substringBefore('\n')
            .substringBefore(" .")
            .substringBefore(" ,")
            // Boilerplate terminator: when the merchant tail runs into the
            // start of the NEXT notification body ("...Nova Sushi Transakcja
            // kartą Nowa transakcja kartą..."), truncate at the first
            // boilerplate word. These are the fixed intro tokens banks use
            // on every notification; if we see them inside a merchant string
            // it's a confident signal that we've crossed into tx N+1.
            .let { truncateAtBoilerplate(it) }
            .trim()
            .trimEnd('.', ',', ';', ':', ' ', '\t', '-', '*', '…')
            // Collapse multiple spaces that the scrub pass may have left.
            .replace(Regex("\\s{2,}"), " ")

        return cleaned.takeIf { isValidMerchant(it) }
    }

    private fun isValidMerchant(s: String): Boolean {
        if (s.length < 2 || s.length > 60) return false
        if (s.all { it.isDigit() || it == ' ' }) return false
        val lowered = s.lowercase()
        if (lowered in setOf("wysokości", "pln", "eur", "usd", "gbp", "zł")) return false
        return true
    }

    /**
     * Truncates the string at the first occurrence of a bank-notification
     * boilerplate intro token. Guards against merchant extraction swallowing
     * the next notification's header text when iOS Vision OCRs multiple
     * stacked notifications as one flat stream.
     *
     * These tokens are picked because they appear only at the START of a
     * notification body — a legitimate merchant name would never contain
     * them. "Transakcja" / "Nowa" are Polish bank boilerplate; "Transaction"
     * / "You paid" are English equivalents.
     */
    private fun truncateAtBoilerplate(s: String): String {
        val boilerplate = Regex(
            "\\s+(?:Transakcja|Nowa\\s+transakcja|Transaction|You\\s+paid|Payment\\s+of)\\b",
            RegexOption.IGNORE_CASE
        )
        val match = boilerplate.find(s) ?: return s
        return s.substring(0, match.range.first)
    }

    /**
     * Strips a leading date-header fragment ("9 October", "9 October | The
     * day before yesterday", "9 października") so the bank-history merchant
     * extractor doesn't prepend date text to the real merchant name.
     *
     * Two-step strip: first the "DAY MONTH" prefix, then the optional
     * "| <relative annotation>" suffix (localized list of known phrases
     * — "The day before yesterday", "3 days", "Yesterday", "Przedwczoraj"
     * etc. — rather than a greedy regex that would otherwise eat merchant
     * text starting with lowercase).
     */
    private fun stripLeadingDateHeader(s: String): String {
        val header = dateHeaderRegex.find(s) ?: return s
        // Only strip if the header appears NEAR the start (within first
        // 60 chars of the slice) — otherwise it's probably in the middle
        // of merchant text (unlikely but defensive).
        if (header.range.first > 60) return s
        var cutIdx = header.range.last + 1
        // Also consume a trailing "| <known relative phrase>" annotation if
        // present directly after the DAY MONTH header.
        val afterHeader = s.substring(cutIdx)
        val suffixMatch = dateHeaderSuffixRegex.find(afterHeader)
        if (suffixMatch != null && suffixMatch.range.first <= 2) {
            cutIdx += suffixMatch.range.last + 1
        }
        return s.substring(cutIdx)
    }

    /**
     * `DAY MONTH` headers. Deliberately strict: just the two tokens,
     * nothing else. The optional "| <relative>" annotation is stripped by
     * [dateHeaderSuffixRegex] in a separate step so merchant text that
     * happens to start with lowercase doesn't get eaten.
     */
    private val dateHeaderRegex = Regex(
        "(?i)\\b(\\d{1,2})\\s+" +
            "(January|February|March|April|May|June|July|August|September|October|November|December|" +
            "stycznia|lutego|marca|kwietnia|maja|czerwca|lipca|sierpnia|września|października|listopada|grudnia)\\b"
    )

    /**
     * Known relative-date annotations VeloBank + other PL/EN bank apps show
     * after a `|` on their date-group headers. Explicit list so we don't
     * accidentally swallow merchant text.
     */
    private val dateHeaderSuffixRegex = Regex(
        "\\s*\\|\\s*(?:" +
            "The\\s+day\\s+before\\s+yesterday|Yesterday|Today|" +
            "\\d+\\s+days?(?:\\s+ago)?|A\\s+week\\s+ago|" +
            "Przedwczoraj|Wczoraj|Dzisiaj|\\d+\\s+dni(?:\\s+temu)?|" +
            "Tydzień\\s+temu|Ponad\\s+tydzień\\s+temu" +
            ")",
        RegexOption.IGNORE_CASE
    )

    /**
     * Finds every date-header occurrence in the corpus. Each returned pair
     * is `(startIndex, LocalDate)`. Year is inferred from "today": a header
     * whose (month, day) is strictly in the future vs. today is assumed to
     * belong to the previous year (handles January screenshots that still
     * list December transactions).
     */
    private fun parseDateHeaders(corpus: String): List<Pair<Int, LocalDate>> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return dateHeaderRegex.findAll(corpus).mapNotNull { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val month = monthFromName(match.groupValues[2]) ?: return@mapNotNull null
            if (day !in 1..31) return@mapNotNull null
            // Try current year first; if the resulting date is in the future
            // relative to today, bump back a year.
            val candidate = runCatching { LocalDate(today.year, month, day) }.getOrNull()
                ?: return@mapNotNull null
            val resolved = if (candidate > today) {
                runCatching { LocalDate(today.year - 1, month, day) }.getOrNull() ?: candidate
            } else {
                candidate
            }
            match.range.first to resolved
        }.toList()
    }

    private fun monthFromName(raw: String): Int? = when (raw.lowercase()) {
        "january", "stycznia" -> 1
        "february", "lutego" -> 2
        "march", "marca" -> 3
        "april", "kwietnia" -> 4
        "may", "maja" -> 5
        "june", "czerwca" -> 6
        "july", "lipca" -> 7
        "august", "sierpnia" -> 8
        "september", "września" -> 9
        "october", "października" -> 10
        "november", "listopada" -> 11
        "december", "grudnia" -> 12
        else -> null
    }

    // ---- date parsing ----

    @Suppress("UnusedParameter")
    private fun resolveDate(
        corpus: String,
        captureEpochSeconds: Long
    ): Pair<LocalDate, Boolean> {
        // User rule: "no date means automatic today". Full stop. We deliberately
        // IGNORE the EXIF capture timestamp — users may screenshot bank history
        // today and import days later, or import a cached screenshot — EXIF
        // date was always the wrong signal. Date headers in the corpus
        // (parsed separately in BANK_HISTORY mode) win; otherwise today.
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date to true
    }

    // ---- confidence ----

    private fun computeConfidence(
        currencyMatched: Boolean,
        merchantFound: Boolean,
        dateInferred: Boolean,
        matchedBankPattern: Boolean
    ): Double {
        var score = 0.3 // amount was at least found (we wouldn't be here otherwise)
        if (matchedBankPattern) score += 0.3 // strong "w wysokości ..." / "amount of ..."
        if (currencyMatched) score += 0.2
        if (merchantFound) score += 0.2
        if (!dateInferred) score += 0.05
        return score.coerceIn(0.0, 1.0)
    }
}

// ============================================================================
// Dedup
// ============================================================================

/**
 * Strict amount-only duplicate detector. The user explicitly asked that
 * dedup trigger on EXACT amount match ONLY — merchant text and date are
 * both too noisy to be reliable identity keys (OCR mangles merchants, dates
 * drift between notification posting and bank settlement). An exact
 * amount-cents match within the DB lookback window is flagged "Possible
 * duplicate" — the user then decides whether to commit anyway.
 *
 * Still rules:
 * - Pass A — against existing DB signatures (lookback bounded by caller).
 * - Pass B — within the current batch (first occurrence wins).
 * - Pass C — tag the survivors as NEW.
 */
class DedupScreenshotCandidatesUseCase {

    operator fun invoke(
        candidates: List<ParsedCandidate>,
        existingSignatures: Set<TxSignature>
    ): List<ParsedCandidate> {
        val seenInBatch = mutableSetOf<TxSignature>()
        return candidates.map { c ->
            if (c.status == ParsedCandidate.Status.PARSE_FAILED) {
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
        return TxSignature(cents)
    }
}

/**
 * Signature used by both the parse-time dedup pass and the live DB query
 * that fetches existing signatures. **Amount-cents ONLY** — see
 * [DedupScreenshotCandidatesUseCase] doc for why.
 */
data class TxSignature(
    val amountCents: Long
)
