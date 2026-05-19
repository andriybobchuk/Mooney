# Mooney Launch-Day Pack

Pre-written copy + an hour-by-hour playbook to fire the moment Apple approves the build. Drafted by the Revenue Strategist agent — meant to be copy-pasted with zero further edits.

**Style rules followed:** No emojis in HN / r/personalfinance / App Store copy. Emojis OK sparingly on Twitter/LinkedIn/TikTok/r/Polska. No buzzwords. No unverifiable claims (no fake user counts, fake testimonials). Privacy mentioned factually once, then moved on.

---

## 1. Show HN post

**Title** (76 chars):

```
Show HN: Mooney – multi-currency finance tracker, no bank linking, built in KMP
```

**Body:**

```
When Mint shut down in early 2024 I went looking for a replacement and couldn't find one that handled my actual situation: I'm Polish, get paid partly in EUR, hold savings in USD, spend in PLN, and send money to family in UAH. Every app I tried either silently converted everything to one currency with a stale rate, refused to add UAH/RUB/AED at all, or wanted me to hand over my bank login to Plaid.

So I spent eight months building Mooney. iOS today, Android pending (Play Console issue on my side, not the app).

A few things that might interest this crowd:

- Single Kotlin codebase, Compose Multiplatform UI on both iOS and Android. Room for local storage, Koin for DI. ~85% code sharing including the UI layer. Happy to answer KMP questions in the comments — there's still very little real-world writeup of shipping Compose to the App Store.

- 17 currencies with dual exchange-rate providers (primary + fallback) so UAH, RUB, AED don't quietly break when one provider drops a pair. Rates cached locally so the app works offline.

- No account creation, no cloud sync, no analytics SDKs. Data lives in Room on device. This is a side effect of being a solo dev with no budget for a backend, not a marketing position.

- Free tier is 20 accounts + 50 custom categories, which covers almost everyone. Pro is 9,99 PLN/month (~$2.50) for unlimited + advanced analytics.

Built solo while keeping a day job. Third App Review resubmission finally cleared this week.

App Store: [link]

Looking for brutal feedback — especially from anyone who's also lived the multi-currency mess. What did Mint do that you still miss?
```

---

## 2. r/Polska post

**Title:**

```
Zbudowałem aplikację do finansów, bo żadna nie ogarniała wielu walut [autopromocja]
```

**Body:**

```
Disclaimer na start: jestem autorem, więc to autopromocja. Zostawiam to do oceny modów.

Sytuacja: dostaję część wynagrodzenia w EUR, oszczędności trzymam w USD, codzienne wydatki w PLN, czasem wysyłam coś w UAH. Próbowałem chyba wszystkiego co jest na App Store — albo aplikacja po cichu przelicza wszystko po kursie sprzed pół roku, albo nie ma UAH w ogóle, albo każe podpinać konto bankowe przez jakiegoś amerykańskiego pośrednika.

Więc przez ostatnie 8 miesięcy po pracy budowałem **Mooney**. Co robi:

- 17 walut, w tym PLN, EUR, USD, UAH, GBP, CHF, AED. Kursy z dwóch źródeł, więc jak jedno padnie to drugie łapie.
- Działa w pełni po polsku (nie maszynowo).
- Bez logowania, bez chmury, bez konta. Dane siedzą lokalnie na telefonie.
- Darmowy plan: 20 kont i 50 własnych kategorii — w sam raz dla większości ludzi.
- Pro: 9,99 zł/mc za nielimit i analizy.

Dziś tylko iOS. Android za chwilę (problem po mojej stronie z Google Play, nie z aplikacją).

App Store: [link]

Jak ktoś też ma życie rozjechane między walutami — daj znać czy działa, i co nie działa. Najbardziej zależy mi na szczerych negatywnych opiniach, bo z pochwał niczego się nie nauczę.
```

---

## 3. r/Ukraine post

**Title:**

```
Зробив застосунок для фінансів з підтримкою гривні і без прив'язки до банку [саморек]
```

**Body:**

```
Одразу скажу — я автор, тож це самореклама. Залишаю на розсуд модераторів.

Контекст: я з Польщі, але півжиття провів між валютами — частина зарплати в EUR, заощадження в USD, витрати в PLN, переказую рідним в UAH. Жоден трекер фінансів, який я пробував, не справлявся з цим нормально. Половина взагалі не має гривні. Друга половина вимагає підключити банк через якогось американського посередника — що для людей з українськими картками просто не працює, не кажучи вже про те що зараз це останнє чого хочеться.

Тому 8 місяців після основної роботи я писав **Mooney**. Що вміє:

- 17 валют, серед них UAH, RUB, PLN, EUR, USD, GBP. Курси з двох джерел, тож якщо одне відвалиться — друге підхопить.
- Повний український інтерфейс, не машинний переклад.
- Жодних логінів, жодної хмари, жодних акаунтів. Дані лежать тільки на твоєму телефоні.
- Безкоштовно: 20 рахунків + 50 категорій. Pro: ~2,50 USD на місяць.

Поки що тільки iOS, Android буде пізніше.

App Store: [link]

Якщо хтось теж живе між кількома країнами і валютами — буду вдячний за чесний фідбек, особливо що не працює. Хвалити не треба, ламати — треба.
```

---

## 4. r/expats post

**Title:**

```
Built a multi-currency finance tracker because every app shows me wrong totals
```

**Body:**

```
Author here, so flag this as self-promo if it doesn't belong.

Pretty sure most people on this sub know the problem: salary in one currency, savings in another, day-to-day spend in a third, plus the occasional transfer back home. Every personal finance app I tried either silently converts everything to one base currency at some random historical rate, or doesn't support half the currencies I actually use (UAH, AED, RUB, etc.), or insists I link my foreign bank through Plaid — which doesn't even exist in half these countries.

Spent 8 months building Mooney as a side project. 17 currencies, dual exchange-rate providers so the less-common ones don't break, all data stays on device (no bank linking, no account, no cloud), Polish and Ukrainian UI included, English of course.

iOS only for now. Free for 20 accounts and 50 categories which is plenty for most. Pro is about $2.50/mo.

App Store: [link]

If you're juggling 3+ currencies I'd love to hear what's broken. Especially edge cases your current app handles badly.
```

---

## 5. r/personalfinance post

**Strict sub. Frame as Mint-refugee discussion, not a launch. Author link goes in Reddit profile bio only — NOT in the post body.**

**Title:**

```
What's everyone using as a Mint replacement two years later?
```

**Body:**

```
Quick disclosure first since this sub is strict and rightly so: I'm a developer and I built one of the alternatives. Not linking it here — if anyone's curious it's in my profile. I'm genuinely more interested in what's actually working for people than in promoting anything.

It's been almost two years since Mint shut down. Rocket Money, Monarch, Copilot, Empower, YNAB, plain spreadsheets — everyone seems to have landed somewhere different and complain about something different.

Curious what stuck for you and what didn't:

- For people who picked a paid one (Monarch, Copilot, YNAB) — was it worth the $100ish/yr or do you regret it?
- For spreadsheet people — what does your setup actually look like in 2026?
- Anyone holding accounts in more than one currency? That's the gap I keep seeing — most of these are pretty US-bank-centric.
```

---

## 6. LinkedIn announcement

### Polish version

```
Mooney jest live w App Store.

Osiem miesięcy temu, po godzinach, zacząłem pisać aplikację do finansów osobistych, bo żadna z istniejących nie ogarniała mojej sytuacji: pensja w kilku walutach, oszczędności w trzeciej, wydatki w czwartej. Mint padł w 2024, polskie aplikacje są albo bankowe, albo bardzo amerykańskie w podejściu.

Co się nauczyłem przez te 8 miesięcy budowania solo:

- Kotlin Multiplatform z Compose Multiplatform realnie się sprawdza. Jeden kod, iOS i Android, ~85% współdzielone razem z UI. To nie jest już eksperyment — można na tym wozić produkt.
- Najwięcej czasu nie zjadł kod, tylko App Review. Trzy resubmisje, w tym jedna z powodu jednej linii w opisie subskrypcji.
- "Build in public" działa wolniej niż obiecują w wątkach na X, ale działa.
- Najtrudniejsza decyzja produktowa nie była techniczna — była dotycząca tego co NIE robić.

Mooney: 17 walut z dwoma dostawcami kursów, polski interfejs, dane lokalnie na urządzeniu, bez kont i bez chmury. Darmowo do 20 kont i 50 kategorii, Pro 9,99 zł/mc.

iOS: [link]

Jak ktoś używa kilku walut na co dzień — chętnie usłyszę co nie gra. Najlepiej szczerze.

#KMP #ComposeMultiplatform #IndieDev #BuildInPublic #PersonalFinance
```

### English version

```
Mooney is live on the App Store today.

Eight months of evenings and weekends. Built solo while keeping a day job. Third App Review submission finally cleared.

A few things I learned shipping this:

- Kotlin Multiplatform + Compose Multiplatform is genuinely production-ready in 2026. ~85% code sharing including the UI layer. iOS and Android from one Kotlin codebase. Room for storage, Koin for DI, kotlinx-serialization throughout. If you're still skeptical about CMP on iOS, this app is a counter-data-point.
- StoreKit subscriptions through KMP is the part nobody writes about. Took longer than the rest of the payment logic combined.
- The slow part of indie dev is not the code. It's the review cycles, the screenshots, the localization passes, the pricing decisions, the App Review rejection that says "your subscription terms are unclear" without telling you which sentence.
- Doing less is a feature. No bank linking, no cloud, no account creation — partly principled, mostly because backends cost money and time I didn't have.

What Mooney does: multi-currency personal finance tracker, 17 currencies with dual exchange-rate providers, Polish and Ukrainian UI alongside English, data stays on device. Free up to 20 accounts and 50 categories. Pro ~$2.50/mo.

App Store: [link]

Would love honest feedback, especially from anyone who's shipped a KMP app or who lives across currencies.

#KotlinMultiplatform #ComposeMultiplatform #IndieHacker #BuildInPublic #iOSDev
```

---

## 7. Twitter/X announcement thread

**Tweet 1 attaches:** the **multi-currency net worth screen** (visual flex — multiple currency balances summed into one net worth figure; most photogenic and differentiated screen).
**Tweet 4 attaches:** the **analytics chart** screen.

```
1/ Mooney is live on the App Store today.

A multi-currency personal finance tracker for people whose money lives in more than one country.

8 months of nights and weekends. Built solo. Single Kotlin codebase, iOS and Android.

[screenshot: net worth screen]
```

```
2/ The why:

Mint died in 2024. I get paid in EUR, save in USD, spend in PLN, send money in UAH. Every replacement either silently converted everything at stale rates, didn't support the currencies I actually use, or wanted my bank login.

So I built the thing I wanted.
```

```
3/ 17 currencies. Dual exchange-rate providers so the less common ones (UAH, RUB, AED) don't quietly break when one provider drops a pair. Rates cached, works offline.

No bank linking. No cloud. No account creation. Data lives in Room on device.
```

```
4/ The stack, for the curious:

Kotlin Multiplatform + Compose Multiplatform for UI on both platforms. Room 2.7 alpha for storage. Koin for DI. ~85% code sharing including the UI.

If you're still on the fence about CMP on iOS in 2026 — it's fine. Ship it.

[screenshot: analytics chart]
```

```
5/ Free tier covers most people: 20 accounts, 50 custom categories.

Pro is 9,99 PLN/mo (~$2.50). Unlimited accounts, advanced analytics, recurring transactions.

iOS today. Android pending — Play Console issue on my end, not the app.
```

```
6/ Took three App Review resubmissions. One was for a single ambiguous sentence in the subscription description. The code was the easy part.

Build in public was real this time. Thanks to everyone who replied to half-finished screenshots over the past 8 months.
```

```
7/ App Store: [link]

If you live across currencies I'd love to hear what's broken. Honest feedback over polite feedback, always.
```

---

## 8. Pinned tweet (evergreen)

```
Mooney — a multi-currency personal finance tracker for people whose money lives in more than one country.

17 currencies. No bank linking. No cloud. Data stays on your phone.

Built solo in Kotlin Multiplatform. iOS today.

[App Store link]
```

(248 chars including link placeholder.)

---

## 9. Email to the waitlist

**Subject line:**

```
Mooney is live (and thank you for waiting)
```

**Body:**

```
Hi —

Short one. Mooney is finally live on the App Store. You signed up for the waitlist a while ago and I never want to be the founder who forgets the people who showed up first, so you're getting this before anything else goes out today.

Link: [App Store URL]

The free tier covers most people: 20 accounts, 50 custom categories, all 17 currencies, full Polish and Ukrainian UI. If you want unlimited accounts and the analytics, Pro is 9,99 PLN/mo (~$2.50). No trial gimmicks — you can use it free for as long as you want and upgrade only if you actually need it.

One ask: if Mooney ends up being useful to you, leave a review. App Store reviews from real people in the first week make or break whether the algorithm shows this to anyone else.

If it's not useful, reply to this email and tell me why. I read everything.

Built this for the version of me who couldn't find what he needed. Hope it works for you too.

— Andriy
```

---

## 10. DM templates for YouTube reviewers

### English version

```
Hi [Creator Name],

I watched your video on [specific video reference] — the bit about [one concrete detail from the video, e.g. "how Copilot still has no UAH support"] is exactly the gap I tried to fill.

I just shipped Mooney on iOS. It's a multi-currency personal finance tracker — 17 currencies including UAH, RUB, AED with dual exchange-rate providers, no bank linking, all data on device. Built solo over 8 months in Kotlin Multiplatform (one codebase, iOS and Android).

Three things I'd like to offer, in order of usefulness to you:

1. A 15-minute interview about shipping a KMP app to the App Store solo — happy to share the rejection saga, the subscription submission mess, the stack. Could be a standalone video or a B-roll segment.
2. Lifetime Pro on your account.
3. 5 lifetime Pro codes for an audience giveaway.

App Store: [link]

No worries if any/all of this isn't a fit. Either way thanks for the work you make.

— Andriy
```

### Polish version

```
Cześć [Creator Name],

Oglądałem twój materiał o [konkretne nawiązanie do filmu] — szczególnie ten moment kiedy [konkret z filmu, np. "wspominałeś że żadna z appek nie obsługuje normalnie kilku walut"]. To dokładnie ta luka, którą próbowałem zamknąć.

Wypuściłem właśnie Mooney na iOS. Trackera finansów wielowalutowy — 17 walut z PLN, EUR, USD, UAH, RUB, polski interfejs, dane lokalnie na telefonie, bez logowania do banku. Pisałem go 8 miesięcy po pracy w Kotlin Multiplatform (jeden kod, iOS i Android).

Mam dla ciebie trzy rzeczy, od najbardziej przydatnej:

1. 15-minutowy wywiad o tym jak wygląda wypuszczenie aplikacji KMP do App Store w pojedynkę — mogę pokazać kulisy odrzuceń, problem z subskrypcjami StoreKit, cały stack. Może być osobny materiał albo wstawka.
2. Lifetime Pro na twoje konto.
3. 5 kodów lifetime Pro do rozdania widzom.

App Store: [link]

Jak nic z tego nie pasuje — żaden problem. I tak dzięki za to co robisz.

— Andriy
```

---

## 11. Landing-page CTA copy

### Variant A — RUN THIS FIRST

```
Headline:    Your money lives in three currencies. Your app should too.
Sub:         Mooney is a multi-currency personal finance tracker for people who hold money in more than one country. 17 currencies. No bank linking. Data on your phone.
Button:      Get it on the App Store
```

### Variant B

```
Headline:    The personal finance app for people who don't fit in one country.
Sub:         17 currencies, dual exchange-rate providers, Polish and Ukrainian UI. No accounts, no cloud, no bank logins. Built solo.
Button:      Download on iOS
```

### Variant C

```
Headline:    Mint shut down. We hold money in more currencies than ever. Here's what we built instead.
Sub:         A private, local-first finance tracker that handles 17 currencies properly — including UAH, RUB, AED.
Button:      See it on the App Store
```

**Why A wins:** It states the user's reality in the first sentence (most landing-page headlines describe the product, not the user). "Your money lives in three currencies" is concrete and personal — anyone in the target audience reads it and silently counts. Sub-headline does the product positioning. B is too dev-y. C buries the lede in nostalgia and forces the user to do the math.

---

## 12. Launch-morning order of operations

Assumes Apple approval lands overnight, Andriy wakes up to a green status, and starts at **08:00 Warsaw time (02:00 ET)**. Goal: by **15:00 Warsaw (09:00 ET)** the launch is fully fanned out and the rest of the day shifts to reply mode.

| Time (Warsaw) | Action | Target | Expected result by 15:00 |
|---|---|---|---|
| **08:00** | Verify App Store listing is live in PL, US, UA storefronts. Search "Mooney" in each — confirm it shows up. Screenshot the listing for later social posts. | App Store | Listing confirmed in 3+ storefronts |
| **08:15** | Update landing page (mooney.app) — replace any "coming soon" with the App Store badge + link. Update OG image. | mooney.app | Landing page is launch-ready |
| **08:30** | Send waitlist email (section 9). Send it now while everyone else's inbox is still quiet. | Email tool | 5–15% open rate in first 2 hours; a handful of installs from the most invested users |
| **09:00** | Post Twitter/X thread (section 7). Pin tweet 1 of the launch thread for today (replace with the evergreen pinned tweet from section 8 after ~24h). | x.com/andriybobchuk | 30–100 impressions, 3–10 likes, maybe 1–2 replies by 15:00. This is fine. |
| **09:15** | Post LinkedIn Polish version (section 6). Polish version first because PL network is more responsive and gives the post early reactions, which lifts the English one. | LinkedIn | 200–500 impressions, 5–15 reactions, 1–3 comments by 15:00 |
| **09:30** | Post LinkedIn English version (section 6). Different audience, won't cannibalize. | LinkedIn | 500–1500 impressions, 10–25 reactions by 15:00 |
| **10:00** | Post r/Polska submission (section 2). Reddit traffic is highly time-of-day sensitive — 10am Warsaw catches the lunch-break PL Reddit peak. | reddit.com/r/Polska | Upvotes will tell you everything in the first 90 minutes. Aim for >70% upvote ratio and 5+ comments by 15:00. If downvoted hard, delete and don't try again the same day. |
| **10:30** | Post Show HN (section 1). Submitting between 09:00–11:00 ET typically gives the most exposure, but 04:30 ET (10:30 Warsaw) is a known sweet spot because early EU upvotes compound when US wakes up. Submit, then stop refreshing. | news.ycombinator.com | Realistic: stays on /newest with 1–5 upvotes. Aspirational: 20+ points and front-page entry. Either outcome is fine — even a quiet Show HN gives you a permanent link for future references. |
| **11:00** | Send the 3 reviewer DMs (section 10). Use the localized version per creator. Don't batch — personalize the `[video reference]` for each. | YouTube / Twitter / Email | Realistic: 1 reply within 48h, not today. Goal today is just to send them. |
| **11:30** | Post r/Ukraine submission (section 3). | reddit.com/r/Ukraine | Smaller sub, slower burn. Aim for >75% upvote ratio and a few comments by 15:00. |
| **12:00** | Lunch + reply break. Reply to every Twitter/LinkedIn/Reddit comment that came in. Replying within 1 hour of a comment is what makes the algorithm push posts further. | Everywhere | All comments answered, no orphan threads. |
| **13:00** | Post r/expats submission (section 4). | reddit.com/r/expats | Niche sub, lower volume. Expect 5–20 upvotes and 1–3 thoughtful comments. |
| **13:30** | Post r/personalfinance discussion thread (section 5). DO NOT include a link to Mooney in the post itself — only in your Reddit profile/bio. This is a discussion seed, not a launch post. | reddit.com/r/personalfinance | Goal: stay un-removed by mods. 10+ comments. 0–5 installs trickling in from people who check your profile. |
| **14:00** | Update GitHub profile README and pinned repos with the App Store link. Update Twitter/X bio with App Store link + "shipped Mooney". Update LinkedIn headline. | All profiles | Every channel that points to Andriy now points one click from Mooney. |
| **14:30** | Quick analytics check: App Store Connect downloads in last 6h, RevenueCat dashboard if Pro conversion is tracked, web analytics on mooney.app, post-impression counts on each social channel. Write down the numbers in a notebook — not for posting, for your own baseline. | App Store Connect, RevenueCat, web analytics | A snapshot you can compare against day 2, day 7, day 30. |
| **15:00** | Stop launching. Start replying. The rest of the day is purely: answer every comment, DM, email, review reply within 1 hour. No new launches today. | Inbox + comments | Inbox at zero. Every commenter feels heard. Day 2 plan written down before bed. |

**What success looks like by 15:00 Warsaw:**

- 20–80 installs from waitlist + organic social
- 3–8 Reddit threads alive with comments
- 1–3 LinkedIn comments asking technical questions
- HN post submitted, regardless of where it ranks
- 0 PR fires (broken App Store link, wrong currency in screenshot, etc.)
- A list of every person who engaged — reply to all of them by end of day

**What is NOT success and is fine:**

- HN doesn't hit front page
- No reviewer responds today
- Under 100 installs on day 1
- A negative Reddit comment

The launch isn't the spike. The launch is the start of the channel — what matters is whether you keep posting on day 8, day 30, day 90.

---

## What's still TODO before launch day arrives

Pre-fill these placeholders in advance so launch morning is pure execution:

- `[link]` → real App Store URL (visible from App Store Connect after approval)
- `[App Store URL]` → same as above
- `[App Store link]` → same
- `[Creator Name]` per DM → fill in personally per reviewer
- `[specific video reference]` / `[konkretne nawiązanie do filmu]` per DM → watch each video, pick one specific moment
- `[one concrete detail from the video]` / `[konkret z filmu]` per DM → from the same watch session
- Screenshot files for Twitter thread (tweet 1 + tweet 4) → export from TestFlight, save as PNG with descriptive filenames
- Landing-page OG image → 1200×630 PNG with the App Store screenshot + headline overlay

When the green status email arrives from Apple, this file is the entire morning playbook.
