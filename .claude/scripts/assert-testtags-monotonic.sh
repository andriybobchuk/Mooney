#!/usr/bin/env bash
#
# Fails if the const-count in TestTags.kt drops below the checked-in
# baseline. Protects against a silent testTag deletion that would break
# the Maestro suite without any typechecker signal.
#
# Baseline lives at .claude/scripts/testtags.baseline. Update it via
# `./.claude/scripts/assert-testtags-monotonic.sh --update` when
# intentionally adding a tag.
#
# CI wiring (future): same job as check-testtag-coverage.

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
testtags_file="$repo_root/composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt"
baseline_file="$repo_root/.claude/scripts/testtags.baseline"

if [[ ! -f "$testtags_file" ]]; then
    echo "assert-testtags-monotonic: TestTags.kt not found at $testtags_file" >&2
    exit 2
fi

# Count `const val` lines — one per Maestro-visible tag.
current=$(grep -c "^\s*const val" "$testtags_file")

if [[ "${1:-}" == "--update" ]]; then
    echo "$current" > "$baseline_file"
    echo "assert-testtags-monotonic: baseline set to $current."
    exit 0
fi

if [[ ! -f "$baseline_file" ]]; then
    echo "assert-testtags-monotonic: no baseline — seeding at $current."
    echo "$current" > "$baseline_file"
    exit 0
fi

baseline=$(cat "$baseline_file")

if [[ "$current" -lt "$baseline" ]]; then
    echo "assert-testtags-monotonic: FAIL — TestTags const count dropped from $baseline to $current." >&2
    echo "  If a tag was intentionally removed (rare), run:" >&2
    echo "    ./.claude/scripts/assert-testtags-monotonic.sh --update" >&2
    exit 1
fi

if [[ "$current" -gt "$baseline" ]]; then
    echo "assert-testtags-monotonic: OK — count grew from $baseline to $current. Updating baseline."
    echo "$current" > "$baseline_file"
    exit 0
fi

echo "assert-testtags-monotonic: OK — count unchanged ($current)."
