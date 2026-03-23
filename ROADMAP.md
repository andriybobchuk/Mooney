# Mooney — Pre-Release Roadmap

The big picture. Each section links to detailed plans where they exist.

---

## 1. Architecture Cleanup (see TECH_DEBT.md)

- [ ] Phase 0: Remove unused code & features (asset analytics, theme switcher, smart suggestions)
- [ ] Phase 1: Critical fixes (runBlocking, layer violations, test code in production)
- [ ] Phase 2: Extract use cases from ViewModels — everything is a use case
- [ ] Phase 3: Slim repository, extract transfer logic
- [ ] Phase 4: Remove GlobalConfig anti-pattern, state management consistency

---

## 2. Dev Infrastructure

- [x] Detekt linter
- [x] CI (lint + build + test on every push)
- [x] Test infrastructure (kotlin-test, Turbine, coroutines-test, fake DAOs)
- [x] Kover coverage reporting
- [x] Claude Code setup (rules, skills, review, architecture enforcement)
- [ ] CD — auto-deploy to Google Play and App Store on push to master
- [ ] Remote Config for extensive feature flag setup
- [ ] A/B testing infrastructure
- [ ] Crashlytics integration
- [ ] Screenshot tests
- [ ] Integration / E2E tests
- [ ] Feature-oriented architecture — any feature can be toggled off via flag

---

## 3. Auth & Cloud

- [ ] Firebase Auth (Google, Apple, Email sign-in)
- [ ] Firebase Firestore as cloud database
- [ ] Data encryption audit — what CAN be stored in DB vs what must be encrypted
- [ ] Local-to-cloud migration tool (read mooney.db → upload to Firestore)
- [ ] Offline-first with sync

---

## 4. UI/UX for MVP

- [ ] Empty state placeholders with CTAs (no data → guide user to add first transaction/account)
- [ ] Frictionless onboarding flow:
  - [ ] Base currency pre-populated from locale
  - [ ] Flag: "Do you pay taxes yourself?" — toggles tax logic throughout app
  - [ ] Quick-start: create first account + first transaction in onboarding
- [ ] Profile button in top right corner → settings & preferences
- [ ] Exchange & Goals tabs disabled by default — popup to enable later or via profile
- [ ] TASTY, minimalistic, modern, premium design pass
  - [ ] Consistent spacing, typography, colors
  - [ ] Smooth animations and transitions
  - [ ] Dark mode polished

---

## 5. Killer Features

- [ ] **Full multicurrency support** — any currency, live exchange rates, convert on the fly
- [ ] **Full custom categories** — user creates/edits/deletes categories and subcategories
- [ ] **Comprehensive analytics** — accounts, expenses, income, taxes with charts and trends
- [ ] **Customizable recurring transactions** — user-defined schedules (monthly rent, subscriptions)
- [ ] **Customizable goals** — with progress tracking and detailed analytics
- [ ] **Cross-platform:** Android (launch), iOS (launch), Web (later), Apple Watch (later)

---

## 6. Launch

- [ ] Google Play Store listing (screenshots, description, privacy policy)
- [ ] App Store listing (screenshots, description, privacy policy)
- [ ] Privacy policy & terms of service
- [ ] Discord server with feedback channel
- [ ] Full rollout to 100% immediately (no staged rollout for v1)

---

*Last updated: 2026-03-23*
