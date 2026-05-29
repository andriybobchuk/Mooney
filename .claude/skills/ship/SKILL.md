---
name: ship
description: Release to TestFlight — verify, bump version in both gradle.properties and the Xcode project, commit, push dev, merge to master, and watch CI. Pushing master triggers a real TestFlight deploy.
allowed-tools: Bash(./gradlew:*), Bash(git:*), Bash(gh:*), Bash(sed:*), Bash(grep:*), Read, Edit
argument-hint: "[new version e.g. 26.05.15 — omit to auto-increment patch]"
user-invocable: true
disable-model-invocation: true
---

# /ship — Release to TestFlight

Encodes the full release dance. **Pushing to `master` triggers an automatic
TestFlight build** — that's a hard-to-reverse action affecting a shared system,
so this skill MUST confirm with the user before the master push.

**Arguments:** `$ARGUMENTS` — the new version (CalVer `YY.MM.PATCH`). If omitted,
auto-increment the patch of the current `app.version`.

## Step 1 — Pre-flight checks

```bash
git branch --show-current          # must be on dev
git status --short                 # review what's uncommitted
grep -E '^app\.version|^app\.versionCode' gradle.properties
```
If not on `dev`, stop and tell the user. If there are unrelated uncommitted changes, list them and confirm what should be included.

## Step 2 — Verify (the CI gate)

Run `/verify` (detekt + assembleDebug + testDebugUnitTest + iOS compile). **Do not proceed if anything fails.** Fix, re-run, only continue when green.

## Step 3 — Bump version in BOTH places

Compute the new version (from `$ARGUMENTS` or patch+1) and the new `app.versionCode` (current+1).

1. Edit `gradle.properties`: set `app.version` and `app.versionCode`.
2. Bump every `MARKETING_VERSION` in the Xcode project:
```bash
sed -i '' "s/MARKETING_VERSION = <OLD>/MARKETING_VERSION = <NEW>/g" iosApp/iosApp.xcodeproj/project.pbxproj
grep MARKETING_VERSION iosApp/iosApp.xcodeproj/project.pbxproj | head -2   # confirm
```
`AppVersion.kt` (Settings display) is generated from `app.version` automatically — do not edit it.

## Step 4 — Commit on dev, push

Stage the version files plus any feature files explicitly by name (NEVER `git add .`). Single-line, human commit message, no AI/Co-Authored-By trailers.
```bash
git add gradle.properties iosApp/iosApp.xcodeproj/project.pbxproj <feature files...>
git commit -m "<concise message>"
git push origin dev
```

## Step 5 — CONFIRM, then merge to master

Show the user: new version, commit summary, and that this will trigger TestFlight. **Wait for explicit go-ahead.** Then:
```bash
git checkout master && git merge dev --no-ff -m "Release <NEW>" && git push origin master
git checkout dev
```

## Step 6 — Watch CI

```bash
gh run list --branch master --limit 2
```
Report the run ID. If `gh` is unauthenticated, tell the user to run `! gh auth login`. The CI `Lint, Build & Test` job must pass before TestFlight/Play deploy jobs run; a green `upload_to_testflight` step means the build is on TestFlight even if a later dSYM step is red.

## Reminders
- If a new `Secrets.kt` constant was added since last release, confirm it's in the three generation blocks of `.github/workflows/ci.yml` first (else CI won't compile).
- Don't bump for doc-only or `.claude/`-only changes — those are `paths-ignore`d by CI anyway.
