#!/usr/bin/env bash
#
# Fails if any file under composeApp/src/commonMain/**/presentation/** contains
# a `Modifier.clickable` or top-level Button/IconButton/OutlinedButton without
# a `mooneyTestTag(` call in the same file.
#
# Not perfect — grep-scoped and file-level (not per-scope). Good enough as a
# canary against the class of regression it targets: someone adds a new
# interactive Composable and forgets to tag it, and the E2E suite starts
# rotting because Maestro can't reach the new element.
#
# Phase 1 usage: `./.claude/scripts/check-testtag-coverage.sh [--warn-only]`
# --warn-only: prints violations but exits 0. Use during rollout while
# tagging catches up. Drop the flag once every presentation file has at
# least one call site.
#
# CI wiring (future):
#   check-testtag-coverage:
#     runs-on: ubuntu-latest
#     steps:
#       - uses: actions/checkout@v4
#       - run: ./.claude/scripts/check-testtag-coverage.sh

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
scan_root="$repo_root/composeApp/src/commonMain/kotlin"

warn_only=false
if [[ "${1:-}" == "--warn-only" ]]; then
    warn_only=true
fi

violations=0
while IFS= read -r file; do
    # Skip files that clearly aren't UI (viewmodels, use cases, models).
    if [[ "$file" != *"/presentation/"* ]]; then
        continue
    fi
    # Does the file have any interactive Composable?
    if ! grep -qE "Modifier\.clickable|\bButton\(|\bIconButton\(|\bOutlinedButton\(|\bFloatingActionButton\(" "$file"; then
        continue
    fi
    # Is there at least one mooneyTestTag call?
    if grep -q "mooneyTestTag(" "$file"; then
        continue
    fi
    printf "  %s\n" "${file#$repo_root/}"
    violations=$((violations + 1))
done < <(find "$scan_root" -name "*.kt" -type f)

if [[ $violations -eq 0 ]]; then
    echo "check-testtag-coverage: OK — every presentation file with interactive Composables has at least one mooneyTestTag."
    exit 0
fi

echo ""
echo "check-testtag-coverage: $violations file(s) have interactive Composables with no mooneyTestTag."
if $warn_only; then
    echo "  (--warn-only, not failing.)"
    exit 0
fi
echo "  Add a mooneyTestTag(TestTags.FOO) call to at least one interactive element per file."
echo "  See composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt"
exit 1
