# Mooney — Pre-Release Roadmap

Priority order: ship first, enhance after. Each section is a milestone.

---

## Milestone 1: Clean Codebase (see TECH_DEBT.md)

- [ ] Phase 0: Remove unused code & features (asset analytics, theme switcher, smart suggestions)
- [ ] Phase 1: Critical fixes (runBlocking, layer violations, test code in production)
- [ ] Phase 2: Extract key use cases from ViewModels — everything is a use case
- [ ] Phase 3: Slim repository, extract transfer logic
- [ ] Phase 4: Remove GlobalConfig anti-pattern, state management consistency

---

## Milestone 2: UI/UX Polish for MVP

- [ ] Empty state placeholders with CTAs (no data → guide user to add first transaction/account)
- [ ] Frictionless onboarding flow:
  - [ ] Base currency pre-populated from locale
  - [ ] "Do you pay taxes yourself?" flag — toggles tax logic throughout app
  - [ ] Quick-start: create first account + first transaction in onboarding
- [ ] Profile button in top right corner → settings & preferences
- [ ] Exchange & Goals tabs disabled by default — popup to enable later or via profile
- [ ] Design pass: consistent spacing, typography, colors, dark mode polished
- [ ] Smooth animations and transitions

---

## Milestone 3: Ship v1.0

- [ ] Google Play Store listing (screenshots, description, privacy policy)
- [ ] App Store listing (screenshots, description, privacy policy)
- [ ] Privacy policy & terms of service
- [ ] Discord server with feedback channel
- [ ] CI gate: lint + build + test must pass before push to master
- [ ] Manual promotion to production (CI builds artifact, you click "promote" in Play Console / App Store Connect)
- [ ] 1-day staged rollout (10% → 100%) to catch crashes

---

## Milestone 4: Post-Launch Foundation (v1.1)

- [ ] Crashlytics integration — see real crash reports from users
- [ ] Simple feature flags in code (`object FeatureFlags { val goalsEnabled = false }`) — no Remote Config yet
- [ ] Firebase Auth (Google, Apple, Email sign-in)
- [ ] Firebase Firestore as cloud database
- [ ] Local-to-cloud migration tool (mooney.db → Firestore)
- [ ] Offline-first with sync
- [ ] Data encryption audit — what can be stored in DB vs what must be encrypted

---

## Milestone 5: Growth & Polish (v1.2+)

- [ ] Remote Config for feature flags (replace hardcoded flags)
- [ ] A/B testing infrastructure (needs users first)
- [ ] CD auto-deploy (CI → staged rollout → auto-promote if no crashes)
- [ ] Screenshot tests (once UI is stable)
- [ ] Integration / E2E tests
- [ ] Feature-oriented architecture — any feature toggled off via remote flag

---

## Killer Features (build incrementally across milestones)

- [ ] **Full multicurrency support** — any currency, live exchange rates, convert on the fly
- [ ] **Full custom categories** — user creates/edits/deletes categories and subcategories
- [ ] **Comprehensive analytics** — accounts, expenses, income, taxes with charts and trends
- [ ] **Customizable recurring transactions** — user-defined schedules (monthly rent, subscriptions)
- [ ] **Customizable goals** — with progress tracking and detailed analytics
- [ ] **Cross-platform:** Android + iOS (v1.0), Web (later), Apple Watch (later)
- [ ] **TASTY, minimalistic, modern, premium design**
- [ ] **Discord server with developer** for direct feedback

---

*Last updated: 2026-03-23*
