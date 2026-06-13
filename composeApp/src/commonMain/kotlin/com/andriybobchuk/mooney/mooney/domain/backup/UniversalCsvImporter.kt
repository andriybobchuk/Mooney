package com.andriybobchuk.mooney.mooney.domain.backup

import kotlinx.datetime.LocalDate

/**
 * Parses arbitrary finance-app CSV exports and maps them into Mooney's
 * transaction model. Tested against the column conventions of Mint, YNAB,
 * Wallet (BudgetBakers), Money Lover, 1Money, Spendee, Toshl, and the
 * generic "Date,Amount,Description" pattern.
 *
 * Strategy:
 *  1. Split the file into rows respecting double-quoted fields with embedded
 *     commas. We don't pull in a full CSV library — embedded apps don't
 *     produce edge cases we can't handle with a small state machine.
 *  2. Detect a header row by checking whether the first row contains any
 *     known column-name token. Files without a header are still importable
 *     (caller can map columns manually).
 *  3. Score each header column against three buckets — `DATE`, `AMOUNT`,
 *     `DESCRIPTION` — and pick the best match per bucket. Categories,
 *     accounts, and notes get auto-mapped too but are optional.
 *  4. Detect the date format (ISO / US slash / EU slash / dot-separated)
 *     by parsing the first valid-looking row.
 *  5. Detect the amount format: handle currency symbols, thousands separators
 *     in either locale, and negative numbers via leading minus or parens.
 *
 * The output [ParsedCsv] tells the caller which columns mapped to what and
 * lets the UI show a preview before committing the import.
 */
@Suppress("ReturnCount", "LoopWithTooManyJumpStatements")
class UniversalCsvImporter {

    data class ParsedCsv(
        val headers: List<String>,
        val rows: List<List<String>>,
        val mapping: ColumnMapping,
        val detectedDateFormat: DateFormat,
        val decimalSeparator: Char,
        /** Sample of the first up-to-10 mapped transactions for preview. */
        val previewTransactions: List<ParsedTransaction>,
        val totalDataRows: Int
    )

    data class ColumnMapping(
        val dateColumn: Int? = null,
        val amountColumn: Int? = null,
        val descriptionColumn: Int? = null,
        val categoryColumn: Int? = null,
        val accountColumn: Int? = null,
        val noteColumn: Int? = null
    ) {
        val isComplete: Boolean get() = dateColumn != null && amountColumn != null
    }

    data class ParsedTransaction(
        val date: LocalDate,
        val amount: Double,
        val description: String,
        val categoryGuess: String?,
        val accountGuess: String?,
        val note: String?
    )

    enum class DateFormat(val sample: String) {
        ISO("2024-01-15"),
        US_SLASH("01/15/2024"),
        EU_SLASH("15/01/2024"),
        EU_DOT("15.01.2024"),
        US_DASH("01-15-2024"),
        UNKNOWN("?")
    }

    fun parse(content: String): ParsedCsv {
        val rawRows = splitCsv(content).filter { it.isNotEmpty() && it.any { c -> c.isNotBlank() } }
        if (rawRows.isEmpty()) {
            return ParsedCsv(
                headers = emptyList(),
                rows = emptyList(),
                mapping = ColumnMapping(),
                detectedDateFormat = DateFormat.UNKNOWN,
                decimalSeparator = '.',
                previewTransactions = emptyList(),
                totalDataRows = 0
            )
        }

        val hasHeader = looksLikeHeader(rawRows.first())
        val headers = if (hasHeader) rawRows.first() else generateGenericHeaders(rawRows.first().size)
        val dataRows = if (hasHeader) rawRows.drop(1) else rawRows

        val mapping = autoMap(headers)
        val dateFormat = detectDateFormat(dataRows, mapping.dateColumn)
        val decimal = detectDecimalSeparator(dataRows, mapping.amountColumn)

        val preview = dataRows.take(10).mapNotNull { row ->
            parseRow(row, mapping, dateFormat, decimal)
        }

        return ParsedCsv(
            headers = headers,
            rows = dataRows,
            mapping = mapping,
            detectedDateFormat = dateFormat,
            decimalSeparator = decimal,
            previewTransactions = preview,
            totalDataRows = dataRows.size
        )
    }

    fun parseAll(
        rows: List<List<String>>,
        mapping: ColumnMapping,
        dateFormat: DateFormat,
        decimalSeparator: Char
    ): List<ParsedTransaction> = rows.mapNotNull { parseRow(it, mapping, dateFormat, decimalSeparator) }

    // -------- internals --------

    private fun splitCsv(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var i = 0
        val current = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        // Normalise CRLF/CR to LF so the parser only deals with one line break.
        val text = content.replace("\r\n", "\n").replace("\r", "\n")
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            // Escaped quote inside a quoted field.
                            field.append('"')
                            i += 2
                            continue
                        }
                        inQuotes = false
                        i++
                        continue
                    }
                    field.append(c)
                    i++
                }
                c == '"' -> {
                    inQuotes = true
                    i++
                }
                c == ',' -> {
                    current.add(field.toString())
                    field.clear()
                    i++
                }
                c == ';' && !current.contains(",") -> {
                    // Some EU exports use ; as separator. Detect on first hit.
                    current.add(field.toString())
                    field.clear()
                    i++
                }
                c == '\n' -> {
                    current.add(field.toString())
                    field.clear()
                    rows.add(current.toList())
                    current.clear()
                    i++
                }
                else -> {
                    field.append(c)
                    i++
                }
            }
        }
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            rows.add(current.toList())
        }
        return rows
    }

    private fun looksLikeHeader(row: List<String>): Boolean {
        val joined = row.joinToString(" ").lowercase()
        // Any common header word counts as a header — even one match means
        // the file probably has a header line. Otherwise we treat row 1 as data.
        return HEADER_TOKENS.any { token -> joined.contains(token) }
    }

    private fun generateGenericHeaders(count: Int): List<String> =
        (1..count).map { "Column $it" }

    private fun autoMap(headers: List<String>): ColumnMapping {
        fun scoreColumn(header: String, bucket: Bucket): Int {
            val lower = header.lowercase().trim()
            return bucket.tokens.maxOfOrNull { token ->
                when {
                    lower == token -> 100
                    lower.startsWith(token) || lower.endsWith(token) -> 80
                    lower.contains(token) -> 60
                    else -> 0
                }
            } ?: 0
        }
        fun bestColumn(bucket: Bucket): Int? {
            val scored = headers.mapIndexed { idx, h -> idx to scoreColumn(h, bucket) }
            val best = scored.maxByOrNull { it.second } ?: return null
            return best.first.takeIf { best.second > 0 }
        }
        return ColumnMapping(
            dateColumn = bestColumn(Bucket.DATE),
            amountColumn = bestColumn(Bucket.AMOUNT),
            descriptionColumn = bestColumn(Bucket.DESCRIPTION),
            categoryColumn = bestColumn(Bucket.CATEGORY),
            accountColumn = bestColumn(Bucket.ACCOUNT),
            noteColumn = bestColumn(Bucket.NOTE)
        )
    }

    private fun detectDateFormat(dataRows: List<List<String>>, dateCol: Int?): DateFormat {
        if (dateCol == null) return DateFormat.UNKNOWN
        for (row in dataRows.take(10)) {
            val v = row.getOrNull(dateCol)?.trim() ?: continue
            if (v.isBlank()) continue
            when {
                v.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}.*")) -> return DateFormat.ISO
                v.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}.*")) -> {
                    // Could be US or EU. Heuristic: if any first part > 12,
                    // it's EU (day first). Otherwise default to US.
                    val parts = v.split("/")
                    val first = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    return if (first > 12) DateFormat.EU_SLASH else DateFormat.US_SLASH
                }
                v.matches(Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}.*")) -> return DateFormat.EU_DOT
                v.matches(Regex("\\d{1,2}-\\d{1,2}-\\d{4}.*")) -> return DateFormat.US_DASH
            }
        }
        return DateFormat.UNKNOWN
    }

    private fun detectDecimalSeparator(dataRows: List<List<String>>, amountCol: Int?): Char {
        if (amountCol == null) return '.'
        // EU often: 1.234,56 — period as thousands, comma as decimal.
        // US: 1,234.56 — comma as thousands, period as decimal.
        // If we see at least one amount with both, decimal is the LAST.
        for (row in dataRows.take(20)) {
            val v = row.getOrNull(amountCol)?.trim() ?: continue
            if (v.contains('.') && v.contains(',')) {
                return if (v.lastIndexOf('.') > v.lastIndexOf(',')) '.' else ','
            }
        }
        // Fallback: if ANY row has comma only with 1-2 trailing digits, it's
        // probably a EU decimal.
        for (row in dataRows.take(20)) {
            val v = row.getOrNull(amountCol)?.trim() ?: continue
            if (v.matches(Regex("[\\-+]?\\d+,\\d{1,2}\\s*\\w*"))) return ','
        }
        return '.'
    }

    private fun parseRow(
        row: List<String>,
        mapping: ColumnMapping,
        dateFormat: DateFormat,
        decimalSep: Char
    ): ParsedTransaction? {
        val dateCol = mapping.dateColumn ?: return null
        val amountCol = mapping.amountColumn ?: return null
        val date = parseDate(row.getOrNull(dateCol), dateFormat) ?: return null
        val amount = parseAmount(row.getOrNull(amountCol), decimalSep) ?: return null
        val description = mapping.descriptionColumn?.let { row.getOrNull(it) }?.trim().orEmpty()
        val categoryGuess = mapping.categoryColumn?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotBlank() }
        val accountGuess = mapping.accountColumn?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotBlank() }
        val note = mapping.noteColumn?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotBlank() }
        return ParsedTransaction(
            date = date,
            amount = amount,
            description = description,
            categoryGuess = categoryGuess,
            accountGuess = accountGuess,
            note = note
        )
    }

    private fun parseDate(raw: String?, format: DateFormat): LocalDate? {
        if (raw.isNullOrBlank()) return null
        // Strip time component if present — we only care about the date.
        val v = raw.trim().substringBefore(' ').substringBefore('T')
        return runCatching {
            when (format) {
                DateFormat.ISO -> LocalDate.parse(v)
                DateFormat.US_SLASH -> {
                    val (m, d, y) = v.split("/").map { it.toInt() }
                    LocalDate(y, m, d)
                }
                DateFormat.EU_SLASH -> {
                    val (d, m, y) = v.split("/").map { it.toInt() }
                    LocalDate(y, m, d)
                }
                DateFormat.EU_DOT -> {
                    val (d, m, y) = v.split(".").map { it.toInt() }
                    LocalDate(y, m, d)
                }
                DateFormat.US_DASH -> {
                    val (m, d, y) = v.split("-").map { it.toInt() }
                    LocalDate(y, m, d)
                }
                DateFormat.UNKNOWN -> LocalDate.parse(v)
            }
        }.getOrNull()
    }

    private fun parseAmount(raw: String?, decimalSep: Char): Double? {
        if (raw.isNullOrBlank()) return null
        // Strip everything that isn't a digit, period, comma, minus, or paren.
        var v = raw.trim()
        val isNegative = v.startsWith('(') && v.endsWith(')')
        if (isNegative) v = v.drop(1).dropLast(1)
        v = v.replace(Regex("[^0-9.,\\-+]"), "")
        val cleaned = if (decimalSep == ',') {
            v.replace(".", "").replace(',', '.')
        } else {
            v.replace(",", "")
        }
        val parsed = cleaned.toDoubleOrNull() ?: return null
        return if (isNegative) -parsed else parsed
    }

    private enum class Bucket(val tokens: List<String>) {
        DATE(listOf("date", "data", "fecha", "datum", "transactiondate", "posted", "time", "datetime", "wann")),
        AMOUNT(listOf("amount", "value", "sum", "money", "total", "betrag", "kwota", "сумма", "金额", "cantidad", "importe", "gross")),
        DESCRIPTION(listOf("description", "memo", "title", "payee", "merchant", "name", "what", "for", "details", "opis", "описание", "beschreibung", "descripcion", "descripción")),
        CATEGORY(listOf("category", "class", "type", "tag", "kategoria", "категория", "kategorie", "categoria", "categoría")),
        ACCOUNT(listOf("account", "from", "wallet", "card", "konto", "konta", "счёт", "счет", "cuenta")),
        NOTE(listOf("note", "comment", "notes", "kommentar", "комментарий"))
    }

    private companion object {
        val HEADER_TOKENS = listOf(
            "date", "amount", "description", "category", "type", "note",
            "data", "kwota", "kategoria", "opis",
            "fecha", "cantidad", "categoria",
            "datum", "betrag", "beschreibung", "kategorie",
            "сумма", "категория", "описание", "дата"
        )
    }
}
