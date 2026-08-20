#!/bin/sh
# Verifies the applid lookup chain in the api-layer start.sh scripts.
# Asserts the three-state (and one empty-string) truth table that the
# shipyard must honor:
#   AC1 camelCase set, no lowercase      -> camelCase value
#   AC2 lowercase set, no camelCase      -> lowercase value (backward compat)
#   AC3 neither set                       -> IZUDFLT (default)
#   AC4 camelCase set to empty, lowercase set -> lowercase value
#
# Usage: verify-zOSMF-applid-env.sh <path-to-start.sh>
#
# Implementation: extract the shell parameter-expansion expression that
# sets -Dapiml.security.zosmf.applid, then invoke it in a fresh bash
# subshell after setting the relevant env vars. POSI
# ${VAR1:-${VAR2:-DEFAULT}} is valid bash when written in source.

set -eu

start_sh="${1:-}"
if [ -z "$start_sh" ] || [ ! -f "$start_sh" ]; then
    echo "FAIL: usage: $0 <path-to-start.sh>" >&2
    exit 2
fi

# Pull the applid -D line out of start.sh. It looks like:
#     -Dapiml.security.zosmf.applid=${ZWE_zOSMF_applId:-${ZWE_zosmf_applId:-IZUDFLT}} \
# Then reduce it to just the shell parameter-expansion expression:
#     ${ZWE_zOSMF_applId:-${ZWE_zosmf_applId:-IZUDFLT}}
expr=$(grep -E 'apiml\.security\.zosmf\.applid=' "$start_sh" \
       | head -n 1 \
       | sed -E 's/^[[:space:]]*-D[^=]+=//; s/[[:space:]]+\\?[[:space:]]*$//')

if [ -z "$expr" ]; then
    echo "FAIL: applid lookup expression not found in $start_sh" >&2
    exit 3
fi

# Sanity check: the extracted expression must mention BOTH the camelCase
# and the lowercase env-var names. This guards against future regressions
# where someone "simplifies" back to a single variable.
case "$expr" in
    *ZWE_zOSMF_applId*)
        ;;
    *)
        echo "FAIL: extracted expression does not reference camelCase ZWE_zOSMF_applId" >&2
        echo "  expression: $expr" >&2
        exit 4
        ;;
esac
case "$expr" in
    *ZWE_zosmf_applId*)
        ;;
    *)
        echo "FAIL: extracted expression does not reference lowercase ZWE_zosmf_applId (backward-compat fallback)" >&2
        echo "  expression: $expr" >&2
        exit 4
        ;;
esac

echo "Verifying applid lookup in $start_sh"
echo "  expression: $expr"

fail=0

run_case() {
    # Generate a temp script and run it with the env vars set, so the
    # nested ${A:-${B:-C}} expansion is parsed by bash itself (we can't
    # safely concatenate two adjacent parameter substitutions on our end).
    camel="$1"
    lower="$2"
    tmp=$(mktemp)
    cat > "$tmp" <<EOF
#!/bin/bash
export ZWE_zOSMF_applId='$camel'
export ZWE_zosmf_applId='$lower'
printf '%s' "$expr"
EOF
    chmod +x "$tmp"
    out=$("$tmp")
    rm -f "$tmp"
    printf '%s' "$out"
}

assert_eq() {
    label="$1"
    expected="$2"
    actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo "  OK   $label -> $actual"
    else
        echo "  FAIL $label -> expected '$expected', got '$actual'" >&2
        fail=1
    fi
}

assert_eq "AC1 camelCase set, no lowercase"          "IZU"     "$(run_case IZU '')"
assert_eq "AC2 lowercase set, no camelCase (legacy)" "ABC"    "$(run_case '' ABC)"
assert_eq "AC3 neither set (default)"               "IZUDFLT" "$(run_case '' '')"
assert_eq "AC4 camelCase empty, lowercase fallback" "FALL"    "$(run_case '' FALL)"

if [ "$fail" -ne 0 ]; then
    echo "FAIL: $0" >&2
    exit 1
fi
echo "PASS: $0"
