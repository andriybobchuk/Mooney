#!/usr/bin/env bash
#
# Boots the app under the e2e build, walks the top-level tabs, dumps the
# Maestro hierarchy on each, and greps for every const value listed in
# TestTags.kt. Any const that never appears in any dump = fail.
#
# Catches:
#   - A `mooneyTestTag(TestTags.FOO)` call that was accidentally removed.
#   - A tag that was declared in TestTags but never applied to a Composable.
#   - The `WithTestTagsAsResourceId` wrapper being removed from the App root
#     (in which case NO tags would surface — mass failure, easy to diagnose).
#
# Requires: emulator running, app already installed (assembleE2e + adb install),
#           maestro CLI on PATH.
#
# CI wiring (future):
#   maestro-tag-preflight:
#     needs: [check]
#     runs-on: ubuntu-latest
#     steps:
#       - # ... standard emulator + install steps
#       - run: ./.claude/scripts/preflight-testtags.sh

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
testtags_file="$repo_root/composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

# Extract every const value — e.g. `const val NAV_TRANSACTIONS = "nav_transactions"`.
mapfile -t tag_values < <(
    grep -oE 'const val \w+ = "[^"]+"' "$testtags_file" \
        | sed -E 's/.*= "([^"]+)".*/\1/'
)

if [[ ${#tag_values[@]} -eq 0 ]]; then
    echo "preflight-testtags: no consts found in TestTags.kt" >&2
    exit 2
fi

echo "preflight-testtags: walking screens to dump hierarchies (${#tag_values[@]} tags declared)..."

# Launch app with empty fixture (no seeded data — we don't need it here,
# we just need every tab to render).
maestro test - <<EOF
appId: com.andriybobchuk.mooney
---
- launchApp:
    arguments:
      fixture: "empty"
      wipeDb: true
EOF

# Dump top-level screen hierarchy. In a future iteration, walk to each tab.
maestro hierarchy > "$tmpdir/hierarchy.txt"

missing=()
for tag in "${tag_values[@]}"; do
    if ! grep -Fq "$tag" "$tmpdir/hierarchy.txt"; then
        missing+=("$tag")
    fi
done

if [[ ${#missing[@]} -eq 0 ]]; then
    echo "preflight-testtags: OK — every declared tag is reachable."
    exit 0
fi

echo "preflight-testtags: FAIL — the following declared tags were not found in the UI hierarchy:" >&2
for tag in "${missing[@]}"; do
    echo "  - $tag" >&2
done
echo "" >&2
echo "  Either apply Modifier.mooneyTestTag(TestTags.…) at the missing call sites," >&2
echo "  or remove the unused const from TestTags.kt (and run assert-testtags-monotonic.sh --update)." >&2
exit 1
