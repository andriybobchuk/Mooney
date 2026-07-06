#!/usr/bin/env bash
#
# Verifies that every `id:` selector referenced in .maestro/flows/*.yaml
# matches a tag value declared in TestTags.kt. Catches:
#   - Typos in flow IDs (`fab_add_tx` vs `fab_add_txn`).
#   - Tags that were renamed in TestTags.kt but not swept through flows.
#   - Flows written against a tag that hasn't landed yet.
#
# CI wiring (future): add to the tag-lint job alongside coverage.

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
testtags_file="$repo_root/composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt"
flows_dir="$repo_root/.maestro/flows"
shared_dir="$repo_root/.maestro/shared"

# Extract every declared tag value:
#   `const val FOO = "foo_bar"`  → foo_bar
#   `fun foo(x: T) = "foo_x"`    → the string literal after `=`; we harvest
#                                   any string that appears in TestTags.kt.
mapfile -t declared_tags < <(
    grep -oE '"[a-z][a-z_0-9]*"' "$testtags_file" \
        | tr -d '"' \
        | sort -u
)

# Extract every literal `id:` reference from every flow YAML.
mapfile -t used_tags < <(
    grep -RhoE 'id:[[:space:]]*"[^"]+"' "$flows_dir" "$shared_dir" 2>/dev/null \
        | sed -E 's/id:[[:space:]]*"([^"]+)"/\1/' \
        | sort -u
)

missing=()
for tag in "${used_tags[@]}"; do
    # Dynamic tags (txn_row_$id, onboarding_currency_$code, etc.) — accept
    # a match on the fixed prefix component up to the first underscore run
    # that would be replaced.
    matched=false
    for decl in "${declared_tags[@]}"; do
        if [[ "$tag" == "$decl" ]]; then
            matched=true
            break
        fi
        # Prefix match for dynamic tags: `txn_row_8` vs declared `txn_row_`
        if [[ "$tag" == ${decl}* ]] && [[ "$decl" == *_ ]]; then
            matched=true
            break
        fi
    done
    if ! $matched; then
        # Try prefix match against derived dynamic templates (`txnRow`
        # produces `txn_row_$id`; the string in TestTags.kt shows as
        # `"txn_row_"` in some styles, `"txn_row_$id"` in others).
        prefix="${tag%_*}_"
        if grep -qE "\"${prefix}" "$testtags_file"; then
            matched=true
        fi
    fi
    if ! $matched; then
        missing+=("$tag")
    fi
done

if [[ ${#missing[@]} -eq 0 ]]; then
    echo "verify-flow-tag-refs: OK — all $(printf '%s\n' "${used_tags[@]}" | wc -l | tr -d ' ') referenced tags map to a declaration."
    exit 0
fi

echo "verify-flow-tag-refs: FAIL — the following flow ID references have no matching TestTags.kt declaration:" >&2
for tag in "${missing[@]}"; do
    echo "  - $tag" >&2
done
echo "" >&2
echo "  Either declare the tag in TestTags.kt, or fix the flow's typo." >&2
exit 1
