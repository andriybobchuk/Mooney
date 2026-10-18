package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.platform.ScreenshotOcrResult
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParseMode
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParseScreenshotUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParsedCandidate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Locks in parser behavior for real-world bank notification screenshots.
 *
 * Each test case is a verbatim OCR string from an actual VeloBank
 * notification the user tested with — if we break any of these, the Phase 3
 * "ace it" promise is broken.
 */
class ParseScreenshotUseCaseTest {

    private val sut = ParseScreenshotUseCase()

    private fun parse(vararg lines: String): ParsedCandidate =
        parseAll(*lines).first()

    private fun parseAll(vararg lines: String): List<ParsedCandidate> =
        sut(ScreenshotOcrResult(
            imageId = "test",
            textLines = lines.toList(),
            captureEpochSeconds = 0L
        ))

    private fun parseBank(vararg lines: String): List<ParsedCandidate> =
        sut(
            ScreenshotOcrResult(
                imageId = "test",
                textLines = lines.toList(),
                captureEpochSeconds = 0L
            ),
            mode = ParseMode.BANK_HISTORY
        )

    @Test
    fun `velobank Nova Sushi`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 164.00 PLN w Nova Sushi ."
        )
        assertEquals(164.00, c.amount)
        assertEquals(Currency.PLN, c.currency)
        assertEquals("Nova Sushi", c.description)
    }

    @Test
    fun `velobank BOLT with special chars survives`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 23.90 PLN w BOLT.EU/O/2610101312  ."
        )
        assertEquals(23.90, c.amount)
        assertNotNull(c.description, "merchant must not be null for BOLT.EU case")
        assertTrue(c.description!!.contains("BOLT"), "expected BOLT in merchant, got '${c.description}'")
    }

    @Test
    fun `velobank merchant starting with digits`() {
        // "021 KRAKOW BONARKA" starts with a digit — regressed before.
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 185.90 PLN w 021 KRAKOW BONARKA   ."
        )
        assertEquals(185.90, c.amount)
        assertEquals("021 KRAKOW BONARKA", c.description)
    }

    @Test
    fun `velobank PRIMARK with trailing city`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 64.50 PLN w PRIMARK 768 Krakow."
        )
        assertEquals(64.50, c.amount)
        assertEquals("PRIMARK 768 Krakow", c.description)
    }

    @Test
    fun `velobank ZABKA`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 10.48 PLN w ZABKA ZE209 K.3    ."
        )
        assertEquals(10.48, c.amount)
        assertNotNull(c.description)
        assertTrue(c.description!!.startsWith("ZABKA"))
    }

    @Test
    fun `velobank LEWIATAN`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 39.84 PLN w LEWIATAN      ."
        )
        assertEquals(39.84, c.amount)
        assertEquals("LEWIATAN", c.description)
    }

    @Test
    fun `strong bank-pattern match earns high confidence`() {
        val c = parse(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w Starbucks ."
        )
        assertTrue(c.confidence >= 0.7, "expected confidence >= 0.7 for strong pattern, got ${c.confidence}")
    }

    @Test
    fun `english apple pay style`() {
        val c = parse("You paid 12.50 USD at Starbucks")
        assertEquals(12.50, c.amount)
        assertEquals(Currency.USD, c.currency)
        assertEquals("Starbucks", c.description)
    }

    @Test
    fun `empty text produces parse failed`() {
        val c = parse()
        assertEquals(ParsedCandidate.Status.PARSE_FAILED, c.status)
    }

    @Test
    fun `no amount in text produces parse failed`() {
        val c = parse("Just some random text with no money")
        assertEquals(ParsedCandidate.Status.PARSE_FAILED, c.status)
    }

    // ---- MULTI-TRANSACTION SCREENSHOTS ----
    // Real iOS Notification Centre screenshots usually contain 3-10 stacked
    // notification cards. Each OCR'd line becomes part of the flat corpus.
    // These tests lock in that we extract ONE candidate per notification.

    @Test
    fun `four stacked VeloBank notifications all parsed`() {
        val results = parseAll(
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 164.00 PLN w Nova Sushi .",
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 23.90 PLN w BOLT.EU/O/2610101312  .",
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 185.90 PLN w 021 KRAKOW BONARKA   .",
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 64.50 PLN w PRIMARK 768 Krakow."
        )
        assertEquals(4, results.size, "expected 4 candidates for 4 notifications, got ${results.size}")
        val amounts = results.map { it.amount }.toSet()
        assertEquals(setOf(164.00, 23.90, 185.90, 64.50), amounts)
        val merchants = results.map { it.description }
        assertTrue(merchants.any { it == "Nova Sushi" }, "missing Nova Sushi: $merchants")
        assertTrue(merchants.any { it?.contains("BOLT") == true }, "missing BOLT: $merchants")
        assertTrue(merchants.any { it == "021 KRAKOW BONARKA" }, "missing BONARKA: $merchants")
        assertTrue(merchants.any { it == "PRIMARK 768 Krakow" }, "missing PRIMARK: $merchants")
    }

    @Test
    fun `six stacked notifications with amount repeat`() {
        // The 164.00 Nova Sushi tx appears twice in the user's actual
        // screenshot (once in each day's notification stack); the dedup
        // within the image should collapse them to one.
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 10.48 PLN w ZABKA ZE209 K.3    .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 39.84 PLN w LEWIATAN      .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 164.00 PLN w Nova Sushi    .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 23.90 PLN w BOLT.EU/O/2610101312  .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 185.90 PLN w 021 KRAKOW BONARKA   .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 64.50 PLN w PRIMARK 768 Krakow."
        )
        assertEquals(6, results.size, "expected 6 distinct candidates, got ${results.size}")
    }

    @Test
    fun `same amount different merchants parsed as separate candidates`() {
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 50.00 PLN w ZABKA .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 50.00 PLN w LEWIATAN ."
        )
        assertEquals(2, results.size, "expected 2 candidates (different merchants), got ${results.size}")
        assertEquals(setOf("ZABKA", "LEWIATAN"), results.mapNotNull { it.description }.toSet())
    }

    @Test
    fun `exact duplicate amount+merchant in same screenshot collapses to one`() {
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w Starbucks .",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w Starbucks ."
        )
        assertEquals(1, results.size, "expected 1 candidate (within-image dedup), got ${results.size}")
    }

    @Test
    fun `merchant of first candidate does not swallow second notification boilerplate`() {
        // Regression guard: without the bounded merchant search, the
        // merchant of "Nova Sushi" would include "Transakcja kartą Nowa
        // transakcja kartą..." all the way through the next notification.
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 164.00 PLN w Nova Sushi",
            "Transakcja kartą",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 23.90 PLN w LEWIATAN"
        )
        assertEquals(2, results.size)
        val first = results.first { it.amount == 164.00 }
        assertEquals("Nova Sushi", first.description)
    }

    @Test
    fun `multi-tx preserves original left-to-right order`() {
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 10.00 PLN w Alpha",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 20.00 PLN w Beta",
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 30.00 PLN w Gamma"
        )
        assertEquals(listOf(10.0, 20.0, 30.0), results.map { it.amount })
        assertEquals(listOf("Alpha", "Beta", "Gamma"), results.map { it.description })
    }

    // ---- CURRENCY + FORMATTING VARIANTS ----

    @Test
    fun `polish comma decimal separator`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42,50 PLN w Starbucks")
        assertEquals(42.50, c.amount)
        assertEquals(Currency.PLN, c.currency)
    }

    @Test
    fun `mixed currencies in one screenshot`() {
        val results = parseAll(
            "Nowa transakcja kartą 5575 **** **** 8308 w wysokości 10.00 PLN w Alpha",
            "You paid 20.00 EUR at Beta"
        )
        assertEquals(2, results.size)
        assertEquals(Currency.PLN, results[0].currency)
        assertEquals(Currency.EUR, results[1].currency)
    }

    @Test
    fun `polish zł glyph variant`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 15,99 zł w Biedronka")
        assertEquals(15.99, c.amount)
        assertEquals(Currency.PLN, c.currency)
        assertEquals("Biedronka", c.description)
    }

    // ---- OCR NOISE RESILIENCE ----

    @Test
    fun `card number with mangled stars does not pollute amount parsing`() {
        // Sometimes OCR reads `****` as `xxxx` or digit sequences; the scrub
        // pass should still recognize the "4-group" card shape and remove it.
        val c = parse("Nowa transakcja kartą 5575 xxxx xxxx 8308 w wysokości 25.00 PLN w Rossmann")
        assertEquals(25.00, c.amount)
        assertEquals("Rossmann", c.description)
    }

    @Test
    fun `multiple whitespace between tokens still parses`() {
        val c = parse("Nowa  transakcja  kartą  5575  ****  ****  8308  w  wysokości  12.34  PLN  w  Store")
        assertEquals(12.34, c.amount)
        assertEquals("Store", c.description)
    }

    // ---- CONFIDENCE SCORING ----

    @Test
    fun `strong pattern + currency + merchant yields confidence over point eight`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w Lidl")
        assertTrue(c.confidence >= 0.8, "expected >=0.8 for full match, got ${c.confidence}")
    }

    @Test
    fun `bare decimal fallback yields notably lower confidence than strong match`() {
        val weak = parse("random text 42.50")
        val strong = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w Lidl")
        assertTrue(
            weak.confidence < strong.confidence - 0.2,
            "weak=${weak.confidence}, strong=${strong.confidence} — gap too narrow"
        )
    }

    // ---- MERCHANT EDGE CASES ----

    @Test
    fun `merchant with ampersand and apostrophe`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w McDonald's & Co")
        assertEquals(42.50, c.amount)
        assertNotNull(c.description)
        assertTrue(c.description!!.startsWith("McDonald"), "got '${c.description}'")
    }

    @Test
    fun `merchant trailing spaces and dots stripped`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN w LEWIATAN       .")
        assertEquals("LEWIATAN", c.description)
    }

    @Test
    fun `empty merchant tail returns null not crash`() {
        val c = parse("Nowa transakcja kartą 5575 **** **** 8308 w wysokości 42.50 PLN")
        assertEquals(42.50, c.amount)
        // No merchant text follows the amount at all — description null is OK.
        // Importantly: no exception, no empty-string description.
        assertTrue(c.description == null || c.description!!.isNotBlank())
    }

    // ---- BANK-HISTORY LIST FORMAT (VeloBank in-app) ----
    // Totally different layout from notification cards: merchant appears
    // BEFORE the amount, separated by the "Card operation" boilerplate.
    // Date headers ("9 October | The day before yesterday") appear once per
    // transaction group and must bind to the correct candidates.

    @Test
    fun `velobank history single row`() {
        val c = parseBank(
            "9 October | The day before yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN"
        ).first()
        assertEquals(20.95, c.amount)
        assertEquals(Currency.PLN, c.currency)
        assertNotNull(c.description)
        assertTrue(
            c.description!!.contains("UBR") && c.description!!.contains("UBER"),
            "expected UBR*/UBER merchant, got '${c.description}'"
        )
    }

    @Test
    fun `velobank history full screenshot parses every tx`() {
        val results = parseBank(
            "9 October | The day before yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN",
            "8 October | 3 days",
            "PEPCO 110340 KRAKOW 23, K…",
            "Card operation",
            "-227,90 PLN",
            "UBR* PENDING.UBER.COM, Amst…",
            "Card operation",
            "-37,11 PLN",
            "UBR* PENDING.UBER.COM, Ams…",
            "Card operation",
            "-31,94 PLN",
            "7 October",
            "WF 102 KRA BRA POS, KRAKOW,…",
            "Card operation",
            "-6,50 PLN",
            "KAUFLAND 1860, KRAKOW, PL",
            "Card operation",
            "-23,96 PLN",
            "BOLT.EU/O/2610051715, Warsaw,…",
            "Card operation",
            "-15,90 PLN"
        )
        // 7 distinct transactions in the screenshot.
        assertEquals(7, results.size, "expected 7 candidates, got ${results.size}: ${results.map { it.amount to it.description }}")
        val amounts = results.map { it.amount }.toSet()
        assertEquals(setOf(20.95, 227.90, 37.11, 31.94, 6.50, 23.96, 15.90), amounts)
    }

    @Test
    fun `velobank history merchants extracted correctly`() {
        val results = parseBank(
            "8 October | 3 days",
            "PEPCO 110340 KRAKOW 23, K…",
            "Card operation",
            "-227,90 PLN",
            "KAUFLAND 1860, KRAKOW, PL",
            "Card operation",
            "-23,96 PLN"
        )
        val pepco = results.first { it.amount == 227.90 }
        val kaufland = results.first { it.amount == 23.96 }
        assertTrue(
            pepco.description?.startsWith("PEPCO") == true,
            "pepco merchant wrong: '${pepco.description}'"
        )
        assertTrue(
            kaufland.description?.startsWith("KAUFLAND") == true,
            "kaufland merchant wrong: '${kaufland.description}'"
        )
    }

    @Test
    fun `velobank history dates bind to correct txs`() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val results = parseBank(
            "9 October | The day before yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN",
            "8 October | 3 days",
            "PEPCO 110340 KRAKOW 23, K…",
            "Card operation",
            "-227,90 PLN",
            "7 October",
            "KAUFLAND 1860, KRAKOW, PL",
            "Card operation",
            "-23,96 PLN"
        )
        val uber = results.first { it.amount == 20.95 }
        val pepco = results.first { it.amount == 227.90 }
        val kaufland = results.first { it.amount == 23.96 }
        // Expected year: whichever year makes (month=10) not-in-the-future vs today.
        val expectedYear = if (today.monthNumber > 10 ||
            (today.monthNumber == 10 && today.dayOfMonth >= 9)) today.year else today.year - 1
        assertEquals(LocalDate(expectedYear, 10, 9), uber.date)
        assertEquals(LocalDate(expectedYear, 10, 8), pepco.date)
        assertEquals(LocalDate(expectedYear, 10, 7), kaufland.date)
        assertTrue(!uber.dateIsInferred, "uber should use header date, not inferred")
    }

    @Test
    fun `velobank history three UBR rows survive dedup different amounts`() {
        val results = parseBank(
            "8 October | 3 days",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN",
            "UBR* PENDING.UBER.COM, Amst…",
            "Card operation",
            "-37,11 PLN",
            "UBR* PENDING.UBER.COM, Ams…",
            "Card operation",
            "-31,94 PLN"
        )
        // Three different amounts → three candidates (not deduped).
        assertEquals(3, results.size)
    }

    @Test
    fun `velobank history special chars BOLT URL survives`() {
        val c = parseBank(
            "7 October",
            "BOLT.EU/O/2610051715, Warsaw,…",
            "Card operation",
            "-15,90 PLN"
        ).first()
        assertEquals(15.90, c.amount)
        assertNotNull(c.description)
        assertTrue(
            c.description!!.contains("BOLT.EU"),
            "expected BOLT.EU in merchant, got '${c.description}'"
        )
    }

    @Test
    fun `velobank history WF POS merchant with spaces`() {
        val c = parseBank(
            "7 October",
            "WF 102 KRA BRA POS, KRAKOW,…",
            "Card operation",
            "-6,50 PLN"
        ).first()
        assertEquals(6.50, c.amount)
        assertNotNull(c.description)
        assertTrue(
            c.description!!.startsWith("WF 102"),
            "expected 'WF 102 ...' merchant, got '${c.description}'"
        )
    }

    @Test
    fun `velobank history polish month variants`() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val c = parseBank(
            "9 października",
            "ZABKA Z1234, KRAKOW, PL",
            "Operacja kartą",
            "-10,48 PLN"
        ).first()
        assertEquals(10.48, c.amount)
        assertEquals("ZABKA Z1234, KRAKOW, PL", c.description)
        val expectedYear = if (today.monthNumber > 10 ||
            (today.monthNumber == 10 && today.dayOfMonth >= 9)) today.year else today.year - 1
        assertEquals(LocalDate(expectedYear, 10, 9), c.date)
    }

    @Test
    fun `velobank history flat corpus with no newlines still parses`() {
        // iOS Vision sometimes merges multiple rows into one text block.
        // This test verifies the parser handles a joined blob too.
        val results = parseBank(
            "9 October | The day before yesterday UBR* PENDING.UBER.COM, Am… Card operation -20,95 PLN 8 October | 3 days PEPCO 110340 KRAKOW 23, K… Card operation -227,90 PLN"
        )
        assertEquals(2, results.size)
        assertEquals(setOf(20.95, 227.90), results.map { it.amount }.toSet())
    }

    // ---- TRAINING SET: all merchants + edge cases from the user's screenshots ----
    // Each case is a verbatim OCR snippet from a real VeloBank history
    // screenshot. These lock in that the parser handles EVERY merchant +
    // amount format in the user's actual data. If any of these regress, the
    // "feed 50 screenshots at end of month" workflow is broken.

    @Test
    fun `training LinkedIn with star and digits in merchant`() {
        val c = parseBank(
            "LinkedIn*P3099425823, LINKE…",
            "Card operation",
            "-89,99 PLN"
        ).first()
        assertEquals(89.99, c.amount)
        assertTrue(c.description?.startsWith("LinkedIn") == true, "got '${c.description}'")
    }

    @Test
    fun `training ZABKA with store code`() {
        val c = parseBank(
            "ZABKA ZE209 K.1, KRAKOW, PL",
            "Card operation",
            "-27,89 PLN"
        ).first()
        assertEquals(27.89, c.amount)
        assertEquals("ZABKA ZE209 K.1, KRAKOW, PL", c.description)
    }

    @Test
    fun `training Bistro Kuchnia Domowa with Polish chars`() {
        val c = parseBank(
            "Bistro Kuchnia Domowa, Krakow…",
            "Card operation",
            "-104,65 PLN"
        ).first()
        assertEquals(104.65, c.amount)
        assertTrue(c.description?.startsWith("Bistro") == true)
    }

    @Test
    fun `training HERBACIARNIA CZAJNIK uppercase`() {
        val c = parseBank(
            "HERBACIARNIA CZAJNIK, GLIW…",
            "Card operation",
            "-59,00 PLN"
        ).first()
        assertEquals(59.00, c.amount)
        assertTrue(c.description?.startsWith("HERBACIARNIA") == true)
    }

    @Test
    fun `training ZDROFIT HIGH 5IVE with digit in name`() {
        val c = parseBank(
            "ZDROFIT HIGH 5IVE, KRAKOW,…",
            "Card operation",
            "-164,51 PLN"
        ).first()
        assertEquals(164.51, c.amount)
        assertTrue(c.description?.contains("5IVE") == true)
    }

    @Test
    fun `training UKRAINOCZKA GLIWICE repeated city`() {
        val c = parseBank(
            "UKRAINOCZKA GLIWICE, GLIWI…",
            "Card operation",
            "-88,49 PLN"
        ).first()
        assertEquals(88.49, c.amount)
        assertTrue(c.description?.startsWith("UKRAINOCZKA") == true)
    }

    @Test
    fun `training JMP SA BIEDRONKA with periods`() {
        val c = parseBank(
            "JMP S.A. BIEDRONKA 519, KRA…",
            "Card operation",
            "-173,59 PLN"
        ).first()
        assertEquals(173.59, c.amount)
        assertTrue(c.description?.startsWith("JMP") == true)
    }

    @Test
    fun `training Nova Sushi 2 mixed case`() {
        val c = parseBank(
            "Nova Sushi 2, Krakow, PL",
            "Card operation",
            "-155,00 PLN"
        ).first()
        assertEquals(155.00, c.amount)
        assertEquals("Nova Sushi 2, Krakow, PL", c.description)
    }

    @Test
    fun `training Allegro simple name`() {
        val c = parseBank(
            "Allegro, Poznan, PL",
            "Card operation",
            "-148,09 PLN"
        ).first()
        assertEquals(148.09, c.amount)
        assertEquals("Allegro, Poznan, PL", c.description)
    }

    @Test
    fun `training Orange Flex lowercase city`() {
        val c = parseBank(
            "Orange Flex, warszawa, PL",
            "Card operation",
            "-35,00 PLN"
        ).first()
        assertEquals(35.00, c.amount)
        assertEquals("Orange Flex, warszawa, PL", c.description)
    }

    @Test
    fun `training WF 102 KRA BRA VEND`() {
        val c = parseBank(
            "WF 102 KRA BRA VEND, KRAKOW…",
            "Card operation",
            "-5,50 PLN"
        ).first()
        assertEquals(5.50, c.amount)
        assertTrue(c.description?.startsWith("WF 102") == true)
    }

    @Test
    fun `training ZARA HOME with CH prefix`() {
        val c = parseBank(
            "ZARA HOME CH Galeria K, Krak…",
            "Card operation",
            "-89,90 PLN"
        ).first()
        assertEquals(89.90, c.amount)
        assertTrue(c.description?.startsWith("ZARA HOME") == true)
    }

    @Test
    fun `training UniCreditPL star InPost`() {
        val c = parseBank(
            "UniCreditPL*InPost Mob, Krakow…",
            "Card operation",
            "-20,49 PLN"
        ).first()
        assertEquals(20.49, c.amount)
        assertTrue(c.description?.startsWith("UniCreditPL") == true)
    }

    @Test
    fun `training ANTHROPIC star CLAUDE SUB`() {
        val c = parseBank(
            "ANTHROPIC* CLAUDE SUB, S…",
            "Card operation",
            "-507,40 PLN"
        ).first()
        assertEquals(507.40, c.amount)
        assertTrue(c.description?.startsWith("ANTHROPIC") == true)
    }

    @Test
    fun `training UBER star EATS PENDING with extra spaces`() {
        val c = parseBank(
            "UBER   * EATS PENDING, Amster…",
            "Card operation",
            "-77,50 PLN"
        ).first()
        assertEquals(77.50, c.amount)
        assertTrue(c.description?.contains("UBER") == true && c.description?.contains("EATS") == true)
    }

    @Test
    fun `training RESTAURACJA RZEZNIA`() {
        val c = parseBank(
            "RESTAURACJA RZEZNIA, KRA…",
            "Card operation",
            "-327,00 PLN"
        ).first()
        assertEquals(327.00, c.amount)
        assertTrue(c.description?.startsWith("RESTAURACJA") == true)
    }

    @Test
    fun `training PKP Intercity with periods and dashes`() {
        val c = parseBank(
            "PKP Intercity S.A. --O, Warszawa…",
            "Card operation",
            "-68,00 PLN"
        ).first()
        assertEquals(68.00, c.amount)
        assertTrue(c.description?.startsWith("PKP Intercity") == true)
    }

    @Test
    fun `training KWIACIARNIA SOFI FLORA`() {
        val c = parseBank(
            "KWIACIARNIA SOFI FLORA, KR…",
            "Card operation",
            "-210,00 PLN"
        ).first()
        assertEquals(210.00, c.amount)
        assertTrue(c.description?.startsWith("KWIACIARNIA") == true)
    }

    @Test
    fun `training NAKIELNY CAFE RESTO`() {
        val c = parseBank(
            "NAKIELNY CAFE RESTO, KRAK…",
            "Card operation",
            "-22,00 PLN"
        ).first()
        assertEquals(22.00, c.amount)
        assertTrue(c.description?.startsWith("NAKIELNY") == true)
    }

    @Test
    fun `training PIJALNIE CZEKOLADY`() {
        val c = parseBank(
            "PIJALNIE CZEKOLADY SP., Krak…",
            "Card operation",
            "-25,72 PLN"
        ).first()
        assertEquals(25.72, c.amount)
        assertTrue(c.description?.startsWith("PIJALNIE") == true)
    }

    @Test
    fun `training mBank Oddzial`() {
        val c = parseBank(
            "mBank Oddzial Galeria, KRAK…",
            "Card operation",
            "-1000,00 PLN"
        ).first()
        assertEquals(1000.00, c.amount)
        assertTrue(c.description?.startsWith("mBank") == true)
    }

    @Test
    fun `training CARREFOUR`() {
        val c = parseBank(
            "CARREFOUR, KRAKOW, PL",
            "Card operation",
            "-145,62 PLN"
        ).first()
        assertEquals(145.62, c.amount)
        assertEquals("CARREFOUR, KRAKOW, PL", c.description)
    }

    @Test
    fun `training LEWIATAN appears multiple times`() {
        val results = parseBank(
            "LEWIATAN, KRAKOW, PL",
            "Card operation",
            "-83,25 PLN",
            "LEWIATAN, KRAKOW, PL",
            "Card operation",
            "-29,23 PLN"
        )
        // Different amounts → two rows, not deduped within image.
        assertEquals(2, results.size)
        assertEquals(setOf(83.25, 29.23), results.map { it.amount }.toSet())
    }

    // ---- THOUSANDS SEPARATOR (CRITICAL) ----

    @Test
    fun `training large amount with space thousands`() {
        val c = parseBank(
            "VDV POLAND NOVUM LIVIN…",
            "Domestic transfer",
            "-4 374,37 PLN"
        ).first()
        assertEquals(4374.37, c.amount)
        assertTrue(c.description?.startsWith("VDV") == true)
    }

    @Test
    fun `training very large amount with space thousands`() {
        val c = parseBank(
            "SOMEONE PAID RENT",
            "Card operation",
            "-60 000,00 PLN"
        ).first()
        assertEquals(60000.00, c.amount)
    }

    @Test
    fun `training 1000 without space still works`() {
        val c = parseBank(
            "mBank Oddzial Galeria, KRAK…",
            "Card operation",
            "-1000,00 PLN"
        ).first()
        assertEquals(1000.00, c.amount)
    }

    @Test
    fun `training very small amount under 1 PLN`() {
        // 1-cent amounts appear on VeloBank's interest / tax rows.
        val c = parseBank(
            "Bistro Kuchnia Domowa",
            "Card operation",
            "-0,01 PLN"
        ).first()
        assertEquals(0.01, c.amount)
    }

    // ---- SIGN GATE: income + foreign currency rows are SKIPPED in BANK_HISTORY mode ----

    @Test
    fun `training positive PLN amount is ignored in bank mode`() {
        val results = parseBank(
            "BOBCHUK ANDRII",
            "Own transfer",
            "60 000,00 PLN"
        )
        // No `-` sign → income row → not a candidate. Parser returns a
        // PARSE_FAILED placeholder because no valid amount matched.
        assertEquals(1, results.size)
        assertEquals(ParsedCandidate.Status.PARSE_FAILED, results.first().status)
    }

    @Test
    fun `training positive VeloBank interest is ignored`() {
        val results = parseBank(
            "VeloBank S.A.",
            "Odsetki - Kapitalizacja odsetek",
            "134,83 PLN"
        )
        assertEquals(1, results.size)
        assertEquals(ParsedCandidate.Status.PARSE_FAILED, results.first().status)
    }

    @Test
    fun `training USD amount is ignored in bank mode`() {
        val results = parseBank(
            "BOBCHUK ANDRII",
            "Exchange office operation",
            "-20 000,00 USD"
        )
        // Non-PLN currency → ignored even if the sign is negative.
        assertEquals(1, results.size)
        assertEquals(ParsedCandidate.Status.PARSE_FAILED, results.first().status)
    }

    @Test
    fun `training mixed income and expense keeps only expenses`() {
        val results = parseBank(
            "9 October | Yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN",
            "BOBCHUK ANDRII",
            "Own transfer",
            "60 000,00 PLN",
            "UBR* PENDING.UBER.COM, Amst…",
            "Card operation",
            "-37,11 PLN"
        )
        // Expense rows survive, income row (positive 60k) is filtered out.
        assertEquals(2, results.size)
        assertEquals(setOf(20.95, 37.11), results.map { it.amount }.toSet())
    }

    // ---- DATE HEADERS ----

    @Test
    fun `training yesterday header`() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val results = parseBank(
            "9 October | Yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-20,95 PLN"
        )
        val expectedYear = if (today.monthNumber > 10 ||
            (today.monthNumber == 10 && today.dayOfMonth >= 9)) today.year else today.year - 1
        assertEquals(LocalDate(expectedYear, 10, 9), results.first().date)
    }

    @Test
    fun `training month boundary September to October`() {
        val results = parseBank(
            "30 September",
            "PKP Intercity S.A., Warszawa",
            "Card operation",
            "-68,00 PLN",
            "29 September",
            "mBank Oddzial Galeria, KRAK…",
            "Card operation",
            "-1000,00 PLN"
        )
        assertEquals(2, results.size)
        val sep30 = results.first { it.amount == 68.00 }
        val sep29 = results.first { it.amount == 1000.00 }
        assertEquals(9, sep30.date.monthNumber)
        assertEquals(30, sep30.date.dayOfMonth)
        assertEquals(9, sep29.date.monthNumber)
        assertEquals(29, sep29.date.dayOfMonth)
    }

    @Test
    fun `training no date header falls back to today`() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val c = parseBank(
            "ZABKA Z1234, KRAKOW, PL",
            "Card operation",
            "-10,48 PLN"
        ).first()
        assertEquals(today, c.date, "No date in text → must fall back to today")
    }

    // ---- FULL REAL SCREENSHOT (biggest regression guard) ----

    // ---- REAL iOS VISION OCR ORDER TESTS ----
    // iOS Vision reads top-to-bottom, left-to-right. A real bank-list row
    // (merchant top-left, amount top-right, subtitle bottom-left) comes
    // back as [merchant, amount, "Card operation"], NOT the test-order
    // [merchant, "Card operation", amount] I was using before. These guard
    // against regressing on the dominant real-world case.

    @Test
    fun `real ocr order single row produces merchant`() {
        val c = parseBank(
            "ZABKA ZE209 K.1, KRAKOW, PL",
            "-27,89 PLN",
            "Card operation"
        ).first()
        assertEquals(27.89, c.amount)
        assertEquals("ZABKA ZE209 K.1, KRAKOW, PL", c.description)
    }

    @Test
    fun `real ocr order three rows produces all merchants`() {
        val results = parseBank(
            "LinkedIn*P3099425823, LINKE…",
            "-89,99 PLN",
            "Card operation",
            "ZABKA ZE209 K.1, KRAKOW, PL",
            "-27,89 PLN",
            "Card operation",
            "Bistro Kuchnia Domowa, Krakow…",
            "-104,65 PLN",
            "Card operation"
        )
        assertEquals(3, results.size)
        val linkedIn = results.first { it.amount == 89.99 }
        val zabka = results.first { it.amount == 27.89 }
        val bistro = results.first { it.amount == 104.65 }
        assertTrue(linkedIn.description?.startsWith("LinkedIn") == true,
            "LinkedIn merchant wrong: '${linkedIn.description}'")
        assertEquals("ZABKA ZE209 K.1, KRAKOW, PL", zabka.description)
        assertTrue(bistro.description?.startsWith("Bistro") == true,
            "Bistro merchant wrong: '${bistro.description}'")
    }

    @Test
    fun `real ocr order with date header prefix`() {
        val results = parseBank(
            "9 October | Yesterday",
            "UBR* PENDING.UBER.COM, Am…",
            "-20,95 PLN",
            "Card operation"
        )
        assertEquals(1, results.size)
        assertTrue(results.first().description?.startsWith("UBR") == true,
            "got '${results.first().description}'")
    }

    @Test
    fun `real ocr order with single-block merchant plus amount`() {
        // Vision sometimes clusters a visually-adjacent merchant + amount
        // into one text block. Our parser should still extract both halves.
        val c = parseBank(
            "KAUFLAND 1860, KRAKOW, PL -23,96 PLN",
            "Card operation"
        ).first()
        assertEquals(23.96, c.amount)
        assertTrue(c.description?.startsWith("KAUFLAND") == true,
            "got '${c.description}'")
    }

    @Test
    fun `real ocr order with bold-thin amount split by whitespace`() {
        // The bank displays amounts with a BOLD integer and THIN decimal/
        // currency (visually: "-27,89 PLN" where "27" is bold and ",89 PLN"
        // is thin). Vision OCRs those two font weights as SEPARATE text
        // blocks, giving us "-27" and ",89 PLN" which join as "-27 ,89 PLN"
        // with a space in the middle. The amount regex must tolerate that.
        val c = parseBank(
            "ZABKA ZE209 K.1, KRAKOW, PL",
            "-27",
            ",89 PLN",
            "Card operation"
        ).first()
        assertEquals(27.89, c.amount)
        assertEquals("ZABKA ZE209 K.1, KRAKOW, PL", c.description)
    }

    @Test
    fun `real ocr order with History screen title at top`() {
        // The bank app has a "History" heading at the top of the screen
        // which Vision picks up before the first merchant. Should be
        // stripped from the first row's merchant.
        val results = parseBank(
            "History",
            "LIDL, GLIWICE, PL",
            "-138,55 PLN",
            "Card operation"
        )
        assertEquals(1, results.size)
        assertEquals("LIDL, GLIWICE, PL", results.first().description)
    }

    @Test
    fun `training first screenshot full page parses ten expense rows`() {
        val results = parseBank(
            "LinkedIn*P3099425823, LINKE…",
            "Card operation",
            "-89,99 PLN",
            "ZABKA ZE209 K.1, KRAKOW, PL",
            "Card operation",
            "-27,89 PLN",
            "Bistro Kuchnia Domowa, Krakow…",
            "Card operation",
            "-104,65 PLN",
            "UBR* PENDING.UBER.COM, Ams…",
            "Card operation",
            "-14,44 PLN",
            "HERBACIARNIA CZAJNIK, GLIW…",
            "Card operation",
            "-59,00 PLN",
            "ZDROFIT HIGH 5IVE, KRAKOW,…",
            "Card operation",
            "-164,51 PLN",
            "UKRAINOCZKA GLIWICE, GLIWI…",
            "Card operation",
            "-88,49 PLN",
            "UBR* PENDING.UBER.COM, Ams…",
            "Card operation",
            "-36,62 PLN",
            "UBR* PENDING.UBER.COM, Am…",
            "Card operation",
            "-33,93 PLN"
        )
        // 9 distinct amounts, all expenses. Three UBR* rows differ by amount.
        assertEquals(9, results.size, "got amounts: ${results.map { it.amount }}")
        assertEquals(
            setOf(89.99, 27.89, 104.65, 14.44, 59.00, 164.51, 88.49, 36.62, 33.93),
            results.map { it.amount }.toSet()
        )
    }

    @Test
    fun `training transfers and interest screenshot ignores non-expense rows`() {
        val results = parseBank(
            "2 October",
            "BOBCHUK ANDRII",
            "Own transfer",
            "60 000,00 PLN",
            "BOBCHUK ANDRII",
            "Own transfer",
            "-60 000,00 PLN",
            "VDV POLAND NOVUM LIVIN…",
            "Domestic transfer",
            "-4 374,37 PLN",
            "BOBCHUK ANDRII",
            "Exchange office operation",
            "20 000,00 USD",
            "BOBCHUK ANDRII",
            "Exchange office operation",
            "-20 000,00 USD",
            "ANTHROPIC* CLAUDE SUB, S…",
            "Card operation",
            "-507,40 PLN",
            "LEWIATAN, KRAKOW, PL",
            "Card operation",
            "-83,25 PLN"
        )
        // Only the negative PLN rows survive:
        // -60 000,00 (own transfer PLN out), -4 374,37 (domestic transfer),
        // -507,40 (ANTHROPIC), -83,25 (LEWIATAN).
        // The USD rows + positive PLN income are filtered out.
        val amounts = results.map { it.amount }.toSet()
        assertEquals(setOf(60000.00, 4374.37, 507.40, 83.25), amounts)
    }
}
