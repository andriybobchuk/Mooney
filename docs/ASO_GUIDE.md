# Mooney — ASO (App Store Optimization) Guide

Reference document for store listing optimization. Review before every store listing update.

---

## Decisions Made

| Element | Decision |
|---------|----------|
| **App name** | `Mooney: Money Tracker` (22 chars) |
| **iOS subtitle** | `Multicurrency Budget Planner` (29 chars) |
| **Google Play short desc** | `Track expenses & budgets across multiple currencies. Manage accounts, goals & more.` |
| **Icon** | "Mo" in SF Pro Display on solid black background, white text |
| **Category** | Primary: Finance. Secondary: Productivity |

---

## App Name & Title (#1 ranking factor)

Keywords in the title carry ~10% more weight than anywhere else.

- **Decided:** `Mooney: Money Tracker` (22 chars)
- "Money tracker" is high volume, less competitive than "budget tracker" or "expense tracker"
- "Mooney" + "Money" creates natural phonetic repetition — memorable, brandable
- Keep under 30 characters (both stores truncate in search results)
- Don't change title frequently — each change resets keyword indexing for ~7 days

## Subtitle (iOS only, 30 chars)

Second strongest keyword signal. Use keywords that DON'T repeat the title.

- **Decided:** `Multicurrency Budget Planner` (29 chars)
- Captures "multicurrency" (differentiator) + "budget" (high volume) + "planner" (variant)
- Combined with title, ranks for: "money tracker", "multicurrency budget", "budget planner"

## Short Description (Google Play only, 80 chars)

Equivalent of iOS subtitle. Indexed for keywords.

- **Decided:** `Track expenses & budgets across multiple currencies. Manage accounts, goals & more.`

## Keywords Field (iOS only, 100 chars)

Hidden field — users never see it, heavily indexed. Comma-separated, no spaces after commas.

- Use singular forms (`budget` not `budgets` — Apple matches both)
- Don't repeat words from title/subtitle
- **Example:** `finance,money,spending,wallet,income,tax,savings,goal,recurring,PLN,EUR,USD,freelancer,net worth`
- Apple indexes combinations across title + subtitle + keywords. "budget" in title + "tracker" in keywords = ranked for "budget tracker"

## Long Description

- **Google Play:** IS indexed. Put keywords naturally, first 2-3 lines most important (visible before "read more"). Keep keyword density under ~3%.
- **App Store:** NOT indexed. Write purely for conversion — bullet points, benefits, social proof.

---

## Screenshots — 3 Seconds to Convert

90% of users never scroll past screenshot 3. First 2 appear in search results.

### Order:
1. **Screenshot 1:** Killer value prop — "Track spending across currencies" with a beautiful transaction list
2. **Screenshot 2:** "Aha" feature — multi-currency net worth or analytics dashboard
3. **Screenshot 3:** Category/account management
4. **Screenshot 4-5:** Goals, recurring transactions, settings

### Rules:
- One benefit per screenshot, never a feature list
- Short text overlays: "See where your money goes" not "Comprehensive analytics dashboard"
- Show real UI — users want to see what they're downloading
- Dark mode screenshots convert better for finance apps (looks premium)
- Device frame + background color + text overlay + actual app screenshot

### A/B Testing:
- Google Play: Store Listing Experiments (free, built-in)
- App Store: Product Page Optimization
- Even icon color changes can swing install rate 15-30%

---

## Icon

### Decision: "Mo" on black

- **Text:** "Mo" in SF Pro Display font (thin or light weight)
- **Background:** Solid black — premium feel (Uber, Nothing approach)
- **Text color:** White — maximum contrast, readable at any size
- Why "Mo": distinctive, sounds like "money", no major app uses it, more personality than a single "M"

### Rules:
- Must be readable at 16x16px — test by shrinking the icon before finalizing
- No accent colors that are green (screams generic finance)
- No gradients, no shadows, no extra elements — just "Mo" on black
- Consider testing thin vs medium font weight via Google Play Store Listing Experiments
- A/B test against a single "M" variant to see which converts better

---

## Ratings & Reviews

Apps below 4.0 are invisible. Above 4.5 convert 2-3x better.

### Strategy:
- Trigger in-app review after positive moment: 5th transaction, first analytics view, goal completed
- NEVER prompt on first launch or after error
- iOS: `SKStoreReviewController` — limited to 3 prompts per 365 days
- Google Play: In-App Review API — more generous
- Reply to every negative review within 24 hours

### Launch window:
- Review velocity matters more than total count
- 10 reviews in 1 week beats 50 over 6 months
- Ask friends/family to review in first 3 days

---

## Localization

Localizing just metadata (not the app) to 5 languages increases downloads 30-40%.

### Priority languages:
1. English (US + UK)
2. Spanish
3. Portuguese (Brazil)
4. German
5. French
6. Japanese

### What to localize:
- Title, subtitle, keywords, description, screenshot text overlays
- Don't machine-translate keywords — research what users actually search in each locale
- iOS allows different keywords per locale — use this for different long-tail phrases per language

---

## Category Selection

- **Primary:** Finance
- **Secondary:** Productivity or Utilities (less competition, might rank higher)

---

## Google Play vs App Store Differences

| Factor | Google Play | App Store |
|--------|------------|-----------|
| Title indexed | Yes (strongest) | Yes (strongest) |
| Subtitle/Short desc | Short description (80 chars, indexed) | Subtitle (30 chars, indexed) |
| Long description | **Indexed for keywords** | Not indexed |
| Hidden keyword field | No | Yes (100 chars) |
| Backlinks matter | Yes (web authority) | No |
| A/B testing built-in | Yes (Store Listing Experiments) | Yes (Product Page Optimization) |
| Crash rate affects ranking | **Yes, heavily** (Android Vitals) | Less directly |
| Update frequency | Boosts ranking | Boosts ranking |

---

## What Most Developers Miss

1. **Crash rate kills Google Play ranking.** Android Vitals tracks ANR rate, crash rate, excessive wakeups. Fix stability issues before launch.

2. **Retention is a ranking signal.** Day-7 and day-30 retention boost discoverability. Good onboarding = better ASO.

3. **Update every 2-4 weeks.** Both stores boost actively maintained apps.

4. **Web presence helps Google Play.** Landing page linking to Play Store = quality signal for Google's crawlers.

5. **First 72 hours matter most.** Both stores give "new app" boost. Have Discord, Reddit, social media ready on day 1.

6. **Custom Store Listings (Google Play)** — different listing pages for different audiences (freelancer tax tracking vs student budget). Underused, very powerful.

7. **Keyword refresh every 4-6 weeks.** Algorithm needs ~7 days to re-index. Gather data, adjust, repeat.

---

## Mooney-Specific Keyword Ideas

### High-value long-tail (less competition):
- multicurrency expense tracker
- freelancer tax tracker
- PLN budget app / EUR budget app
- personal finance multicurrency
- spending tracker multiple accounts

### Avoid (too competitive for a new app):
- budget app
- expense tracker
- finance app
- money manager

---

*Sources: [Moburst](https://www.moburst.com/blog/app-store-optimization-guide/), [AppTweak](https://www.apptweak.com/en/aso-blog/app-store-ranking-factors), [DotComInfoway](https://www.dotcominfoway.com/blog/aso-in-2026-new-ranking-factors-you-cant-ignore/), [ASO Mobile](https://asomobile.net/en/blog/screenshots-for-app-store-and-google-play-in-2025-a-complete-guide/), [MobileAction](https://www.mobileaction.co/blog/app-store-ranking-factors/)*
