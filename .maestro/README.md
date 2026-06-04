# Mooney UI tests (Maestro)

These flows enforce the **cache-first / shimmer / empty-state** contract:

1. **Cold start** never flashes an empty-state placeholder. The first frame is
   either real content (cache warm from a previous session) or a shimmer
   (true cold start). The "Let's get started" / "No transactions yet" copy
   should never appear during initial load.
2. **Tab switching** paints the previously-loaded snapshot on the very first
   frame. No shimmer flicker, no empty-state flash.
3. **Writes propagate** through the AppDataCache — adding/changing data on
   one screen reflects everywhere without re-entering a loading state.

## Install

```bash
brew install maestro          # macOS
# or
curl -fsSL "https://get.maestro.mobile.dev" | bash
```

Verify:
```bash
maestro --version
```

## Run

With an Android emulator running (or device connected) **and the app installed
with an onboarded test user**:

```bash
# All flows
maestro test .maestro/

# A single flow
maestro test .maestro/tabs-no-flicker.yaml
```

For iOS Simulator: `maestro test .maestro/` works identically once the iOS
app is built and running.

## Flows

| File | What it asserts |
|---|---|
| `cold-start.yaml` | App launch never shows an empty-state placeholder before data lands. |
| `tabs-no-flicker.yaml` | Switching between bottom-nav tabs shows cached content immediately — no shimmer/empty-state flash. |
| `add-transaction-no-reload.yaml` | Adding a transaction does not put other tabs into a loading/empty state on next visit. |

## Limitations

- Maestro identifies UI elements primarily by visible text. Text changes break
  the assertions; if you rename "Transactions" / "Assets" / "Analytics" /
  "Let's get started" / "No analytics yet", update the flows.
- These flows assume an onboarded device (at least one account exists). For
  CI, snapshot a `.../app.databases/mooney.db` from a known seeded state and
  push it before running.
- "Shimmer" itself isn't reliably testable visually — these flows test the
  *contract* (no empty-state flash) rather than the shimmer rendering itself.
