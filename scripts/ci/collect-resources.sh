#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
#
# collect-resources.sh - CI resource accounting collector for zowe/api-layer.
#
# Usage:
#   collect-resources.sh start --label <name> [--out-dir <dir>] [--interval <seconds>]
#   collect-resources.sh mark <phase-name> [--out-dir <dir>]
#   collect-resources.sh stop [--label <name>] [--out-dir <dir>] [--log <gradle-log-file>]
#   collect-resources.sh summarize --log <gradle-log-file> [--out <json-path>] [--label <name>]
#   collect-resources.sh selftest [--out-dir <dir>]
#   collect-resources.sh --help
#
# Out-dir resolution: --out-dir > $CI_RESOURCE_OUT_DIR > ${RUNNER_TEMP}/ci-resources >
#                     ${TMPDIR:-/tmp}/ci-resources
#
# Files written under the out-dir:
#   state.json      sampler state (label, start time, interval, sampler pid, cgroup)
#   samples.jsonl   one JSON object per sampling tick (capped at CI_RESOURCE_SAMPLE_CAP,
#                   default 20000 lines, oldest truncated first)
#   marks.jsonl     phase marks recorded with `mark`
#   resources.json  schemaVersion 1 evidence document (bounded, target < 20 KB)
#   resources.csv   one appended row per run
#
# Design notes:
#   * No `set -e`: `stop` must tolerate every optional metric being unavailable and still
#     exit 0, so errors are handled explicitly at each site instead of aborting.
#   * A metric that cannot be measured is emitted as null, never fabricated.
#   * Per tick the sampler sums the RSS of all matching live processes (a simultaneous
#     sum). It never sums per-process lifetime peaks and calls that a simultaneous peak.
#   * Memory precedence: cgroup v2 memory.peak when readable (kernel high-watermark of the
#     cgroup the sampler runs in - on a CI runner that is the job cgroup, so it is the job
#     peak; on a long-lived host cgroup it covers that cgroup's lifetime, which is noted in
#     the summary). Otherwise the sampled simultaneous sum is used and flagged as a lower
#     bound via peakIsLowerBound.
#   * Secrets: values of env vars whose names match
#     (?i)(password|token|secret|passwd|credential|key) are replaced by *** in every output
#     file, and --password= / -P*password= / sonar.token= / --token= patterns are scrubbed
#     when a command line is copied into output.
#   * The Gradle log (tens of MB) is parsed with a single streaming awk pass; raw log lines
#     are never copied into output.

set -uo pipefail

# awk's printf("%.1f") and similar follow LC_NUMERIC; force C so emitted numbers are
# always dot-decimal and parse as JSON.
export LC_NUMERIC=C

SCRIPT_NAME="$(basename "$0")"
SELF_PATH="$0"
case "$SELF_PATH" in
    /*) : ;;
    *) SELF_PATH="$(pwd)/$SELF_PATH" ;;
esac

SAMPLE_CAP_DEFAULT=20000
SAMPLE_CAP="${CI_RESOURCE_SAMPLE_CAP:-$SAMPLE_CAP_DEFAULT}"
DOCKER_TIMEOUT="${CI_RESOURCE_DOCKER_TIMEOUT:-8}"
DEFAULT_PROC_PATTERN='java|javadoc|gradle|docker|containerd|redis'
PROC_PATTERN="${CI_RESOURCE_PROC_PATTERN:-$DEFAULT_PROC_PATTERN}"
SAMPLER_MAX_LIFETIME="${CI_RESOURCE_MAX_LIFETIME:-43200}"

################################################################################
# small helpers
################################################################################

warn() {
    printf '%s: warning: %s\n' "$SCRIPT_NAME" "$*" >&2
}

die() {
    printf '%s: error: %s\n' "$SCRIPT_NAME" "$*" >&2
    exit 2
}

info() {
    printf '%s\n' "$*"
}

now_epoch_float() {
    local v
    v="$(date +%s.%N 2>/dev/null)" || v=""
    case "$v" in
        '' | *[!0-9.]*) v="$(date +%s)" ;;
    esac
    printf '%s' "$v"
}

iso_utc() {
    date -u +%Y-%m-%dT%H:%M:%SZ
}

num_or_null() {
    case "${1:-}" in
        '' | null) printf 'null' ;;
        *[!0-9.eE+-]*) printf 'null' ;;
        *) printf '%s' "$1" ;;
    esac
}

json_array() {
    if [ "$#" -eq 0 ]; then
        printf '[]'
        return 0
    fi
    printf '%s\n' "$@" | jq -R . | jq -sc .
}

array_count() {
    # $1 = name of an array variable; prints its element count (0 when empty)
    local -n ref="$1"
    printf '%s' "${#ref[@]}"
}

str_or_null() {
    # JSON-encodes the argument, or prints the JSON literal null when empty/absent
    local v="${1:-}"
    if [ -z "$v" ] || [ "$v" = "null" ]; then
        printf 'null'
        return 0
    fi
    printf '%s' "$v" | jq -R .
}

require_jq() {
    command -v jq >/dev/null 2>&1 || die "jq is required but was not found in PATH"
}

resolve_out_dir() {
    local explicit="${1:-}"
    if [ -n "$explicit" ]; then
        printf '%s\n' "$explicit"
        return 0
    fi
    if [ -n "${CI_RESOURCE_OUT_DIR:-}" ]; then
        printf '%s\n' "$CI_RESOURCE_OUT_DIR"
        return 0
    fi
    if [ -n "${RUNNER_TEMP:-}" ]; then
        printf '%s\n' "${RUNNER_TEMP}/ci-resources"
        return 0
    fi
    printf '%s\n' "${TMPDIR:-/tmp}/ci-resources"
}

self_cgroup_path() {
    awk -F: 'NF >= 3 { print $3; exit }' /proc/self/cgroup 2>/dev/null
}

read_cgroup_file() {
    # $1 = cgroup-relative path, e.g. memory.current
    local rel="$1" cg
    cg="$(self_cgroup_path)"
    [ -n "$cg" ] || return 1
    [ -r "/sys/fs/cgroup${cg}/${rel}" ] || return 1
    local v
    v="$(cat "/sys/fs/cgroup${cg}/${rel}" 2>/dev/null)" || return 1
    [ -n "$v" ] || return 1
    printf '%s' "$v"
}

read_cpu_stat() {
    # prints: usageUsec userUsec systemUsec nrThrottled throttledUsec (empty when unreadable)
    local cg f
    cg="$(self_cgroup_path)"
    [ -n "$cg" ] || return 1
    f="/sys/fs/cgroup${cg}/cpu.stat"
    [ -r "$f" ] || return 1
    awk '
        /^usage_usec[ \t]/   { u = $2 }
        /^user_usec[ \t]/    { us = $2 }
        /^system_usec[ \t]/  { sy = $2 }
        /^nr_throttled[ \t]/ { nt = $2 }
        /^throttled_usec[ \t]/ { tu = $2 }
        END {
            if (u == "" && us == "" && sy == "" && nt == "" && tu == "") exit 1
            printf "%s %s %s %s %s\n", u, us, sy, nt, tu
        }
    ' "$f" 2>/dev/null
}

################################################################################
# redaction
################################################################################

secret_env_names() {
    env 2>/dev/null | awk -F= '{ print $1 }' | grep -iE '(password|token|secret|passwd|credential|key)' || true
}

redact_patterns() {
    # scrub well-known credential shapes out of text arriving on stdin
    sed -E \
        -e 's/(--password=)[^[:space:]]+/\1***/g' \
        -e 's/(-P[A-Za-z0-9_.-]*[Pp]assword=)[^[:space:]]+/\1***/g' \
        -e 's/(-P[A-Za-z0-9_.-]*[Tt]oken=)[^[:space:]]+/\1***/g' \
        -e 's/(sonar\.token=)[^[:space:]]+/\1***/g' \
        -e 's/(--token=)[^[:space:]]+/\1***/g' \
        -e 's/([Aa]uthorization: *[Bb]earer )[A-Za-z0-9._-]+/\1***/g'
}

redact_text() {
    # stdin -> stdout. Two independent layers: literal secret env values, then patterns.
    local content name val lower
    content="$(cat)"
    while IFS= read -r name; do
        [ -n "$name" ] || continue
        val="${!name-}"
        # False-positive guards: values shorter than 6 chars, and common boolean/status
        # words, are never credentials but would corrupt numeric/boolean JSON if scrubbed.
        [ "${#val}" -ge 6 ] || continue
        lower="$(printf '%s' "$val" | tr '[:upper:]' '[:lower:]')"
        case "$lower" in
            true | false | yes | no | on | off | null | none | nil | unknown | default | enabled | disabled | auto | empty | unset)
                continue
                ;;
        esac
        content="${content//"$val"/***}"
    done < <(secret_env_names)
    printf '%s' "$content" | redact_patterns
}

redact_file() {
    local f="$1" tmp
    [ -f "$f" ] || return 0
    tmp="${f}.redact.tmp"
    if redact_text <"$f" >"$tmp" 2>/dev/null && [ -s "$tmp" ]; then
        mv -f "$tmp" "$f" 2>/dev/null || rm -f "$tmp"
    else
        rm -f "$tmp"
    fi
    return 0
}

################################################################################
# environment probing
################################################################################

probe_cpu_count() {
    local v=""
    if command -v nproc >/dev/null 2>&1; then
        v="$(nproc 2>/dev/null)"
    fi
    if [ -z "$v" ] && command -v getconf >/dev/null 2>&1; then
        v="$(getconf _NPROCESSORS_ONLN 2>/dev/null)"
    fi
    case "$v" in
        '' | *[!0-9]*) printf 'null' ;;
        *) printf '%s' "$v" ;;
    esac
}

probe_meminfo_kb() {
    # $1 = key without colon, e.g. MemTotal
    local f=/proc/meminfo
    [ -r "$f" ] || return 1
    awk -v k="$1" '$1 == k ":" { print $2; found = 1 } END { if (!found) exit 1 }' "$f" 2>/dev/null
}

probe_mem_total_bytes() {
    local kb=""
    kb="$(probe_meminfo_kb MemTotal)" || kb=""
    if [ -z "$kb" ]; then
        if command -v sysctl >/dev/null 2>&1; then
            kb="$(sysctl -n hw.memsize 2>/dev/null)"
            case "$kb" in
                '' | *[!0-9]*) printf 'null' ;;
                *) printf '%s' "$kb" ;; # already bytes
            esac
            return 0
        fi
        printf 'null'
        return 0
    fi
    printf '%s' "$((kb * 1024))"
}

probe_mem_available_bytes() {
    local kb=""
    kb="$(probe_meminfo_kb MemAvailable)" || kb=""
    [ -n "$kb" ] || {
        printf 'null'
        return 0
    }
    printf '%s' "$((kb * 1024))"
}

probe_jdk_version() {
    command -v java >/dev/null 2>&1 || {
        printf 'null'
        return 0
    }
    local first
    first="$(java -version 2>&1 | head -n 1)"
    [ -n "$first" ] || {
        printf 'null'
        return 0
    }
    printf '%s' "$first"
}

probe_jdk_vendor() {
    command -v java >/dev/null 2>&1 || {
        printf 'null'
        return 0
    }
    local v
    v="$(java -XshowSettings:properties -version 2>&1 | awk -F'= *' '/^[[:space:]]*java.vendor =/{ print $2; exit }')"
    v="$(printf '%s' "$v" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    [ -n "$v" ] || {
        printf 'null'
        return 0
    }
    printf '%s' "$v"
}

detect_docker() {
    command -v docker >/dev/null 2>&1 || return 1
    if command -v timeout >/dev/null 2>&1; then
        timeout "$DOCKER_TIMEOUT" docker version --format '{{.Server.Version}}' >/dev/null 2>&1 || return 1
    else
        docker version --format '{{.Server.Version}}' >/dev/null 2>&1 || return 1
    fi
    return 0
}

probe_container_count() {
    local v=""
    if ! command -v docker >/dev/null 2>&1; then
        printf 'null'
        return 0
    fi
    if command -v timeout >/dev/null 2>&1; then
        v="$(timeout "$DOCKER_TIMEOUT" docker ps -q 2>/dev/null)" || v=""
    else
        v="$(docker ps -q 2>/dev/null)" || v=""
    fi
    case "$v" in
        '') printf '0' ;; # docker present but no running containers
        *) printf '%s' "$(printf '%s\n' "$v" | grep -c .)" ;;
    esac
}

dir_bytes() {
    # $1 = directory; prints bytes or empty when unavailable
    local d="$1"
    [ -n "$d" ] || return 1
    [ -d "$d" ] || return 1
    local out
    if command -v timeout >/dev/null 2>&1; then
        out="$(timeout 20 du -s -B1 "$d" 2>/dev/null)" || return 1
    else
        out="$(du -s -B1 "$d" 2>/dev/null)" || return 1
    fi
    printf '%s\n' "$out" | awk '{ print $1; exit }'
}

gradle_cache_dir() {
    if [ -n "${GRADLE_USER_HOME:-}" ]; then
        printf '%s' "${GRADLE_USER_HOME}/caches"
        return 0
    fi
    if [ -n "${HOME:-}" ]; then
        printf '%s' "${HOME}/.gradle/caches"
        return 0
    fi
    printf ''
}

################################################################################
# sampler
################################################################################

sum_matched_rss() {
    # $1 = extended regex matched against process comm and argv[0] basename
    # $2 = page size in bytes
    # prints: "<sumBytes> <processCount>" measured over one pass of /proc
    local pattern="$1" page="$2" sum=0 count=0 d pid comm first resident
    for d in /proc/[0-9]*; do
        [ -r "$d/statm" ] || continue
        pid="${d#/proc/}"
        [ "$pid" = "$$" ] && continue
        comm=""
        IFS= read -r comm <"$d/comm" 2>/dev/null || comm=""
        first=""
        IFS= read -r -d '' first <"$d/cmdline" 2>/dev/null || first=""
        first="${first##*/}"
        if [[ ! "$comm" =~ $pattern ]] && [[ ! "$first" =~ $pattern ]]; then
            continue
        fi
        resident=""
        IFS=' ' read -r _ resident _ <"$d/statm" 2>/dev/null || resident=""
        case "$resident" in
            '' | *[!0-9]*) continue ;;
        esac
        sum=$((sum + resident * page))
        count=$((count + 1))
    done
    printf '%s %s' "$sum" "$count"
}

docker_stats_once() {
    # Single `docker stats --no-stream` call covering every running container.
    # prints "<sumBytes>\t<count>\t<jsonArray>"
    local raw="" tmp="" sum=0 count=0 json='[]'
    if ! detect_docker; then
        printf '0\t0\t[]'
        return 0
    fi
    if command -v timeout >/dev/null 2>&1; then
        raw="$(timeout "$DOCKER_TIMEOUT" docker stats --no-stream --format '{{.Name}}|{{.MemUsage}}' 2>/dev/null)" || raw=""
    else
        raw="$(docker stats --no-stream --format '{{.Name}}|{{.MemUsage}}' 2>/dev/null)" || raw=""
    fi
    [ -n "$raw" ] || {
        printf '0\t0\t[]'
        return 0
    }
    tmp="$(printf '%s\n' "$raw" | awk -F'|' '
        NF >= 2 {
            usage = $2
            sub(/\/.*$/, "", usage)
            gsub(/^[ \t]+/, "", usage)
            gsub(/[ \t]+$/, "", usage)
            if (usage ~ /GiB$/)      v = substr(usage, 1, length(usage) - 3) * 1073741824
            else if (usage ~ /MiB$/) v = substr(usage, 1, length(usage) - 3) * 1048576
            else if (usage ~ /KiB$/) v = substr(usage, 1, length(usage) - 3) * 1024
            else if (usage ~ /GB$/)  v = substr(usage, 1, length(usage) - 2) * 1000000000
            else if (usage ~ /MB$/)  v = substr(usage, 1, length(usage) - 2) * 1000000
            else if (usage ~ /kB$/)  v = substr(usage, 1, length(usage) - 2) * 1000
            else if (usage ~ /B$/)   v = substr(usage, 1, length(usage) - 1) * 1
            else                     v = 0
            printf "%s\t%d\n", $1, v
        }')"
    [ -n "$tmp" ] || {
        printf '0\t0\t[]'
        return 0
    }
    sum="$(printf '%s\n' "$tmp" | awk -F'\t' '{ s += $2 } END { printf "%d", s + 0 }')"
    count="$(printf '%s\n' "$tmp" | grep -c .)"
    json="$(printf '%s\n' "$tmp" | jq -R 'split("\t") | { name: .[0], memBytes: (.[1] | tonumber) }' | jq -sc . 2>/dev/null)"
    [ -n "$json" ] || json='[]'
    printf '%s\t%s\t%s' "$sum" "$count" "$json"
}

cap_samples() {
    local f="$1" lines
    [ -f "$f" ] || return 0
    lines="$(wc -l <"$f" 2>/dev/null)" || return 0
    case "$lines" in
        '' | *[!0-9]*) return 0 ;;
    esac
    [ "$lines" -gt "$SAMPLE_CAP" ] || return 0
    if tail -n "$SAMPLE_CAP" "$f" >"${f}.tmp" 2>/dev/null; then
        mv -f "${f}.tmp" "$f" 2>/dev/null && : >"${f}.truncated"
    else
        rm -f "${f}.tmp"
    fi
    return 0
}

sample_once() {
    local out_dir="$1" interval="$2" cg="$3" page="$4"
    local ts epoch mem_current mem_avail cpu_line rss_line rss_sum rss_count
    local docker_line container_sum container_count container_json
    local u us sy nt tu

    ts="$(iso_utc)"
    epoch="$(now_epoch_float)"

    mem_current="$(read_cgroup_file memory.current)" || mem_current=""
    mem_avail="$(probe_meminfo_kb MemAvailable)" || mem_avail=""
    [ -n "$mem_avail" ] && mem_avail="$((mem_avail * 1024))"

    rss_line="$(sum_matched_rss "$PROC_PATTERN" "$page")"
    rss_sum="${rss_line%% *}"
    rss_count="${rss_line##* }"

    docker_line="$(docker_stats_once)"
    container_sum="$(printf '%s' "$docker_line" | cut -f1)"
    container_count="$(printf '%s' "$docker_line" | cut -f2)"
    container_json="$(printf '%s' "$docker_line" | cut -f3)"
    [ -n "$container_json" ] || container_json='[]'

    cpu_line="$(read_cpu_stat)" || cpu_line=""
    u=""; us=""; sy=""; nt=""; tu=""
    if [ -n "$cpu_line" ]; then
        # shellcheck disable=SC2086  # deliberate word split of 5 space-separated counters
        set -- $cpu_line
        u="${1:-}"; us="${2:-}"; sy="${3:-}"; nt="${4:-}"; tu="${5:-}"
    fi

    jq -cn \
        --arg ts "$ts" \
        --argjson epoch "$(num_or_null "$epoch")" \
        --argjson memCurrent "$(num_or_null "$mem_current")" \
        --argjson memAvailable "$(num_or_null "$mem_avail")" \
        --argjson rssSum "$(num_or_null "$rss_sum")" \
        --argjson rssCount "$(num_or_null "$rss_count")" \
        --argjson containerSum "$(num_or_null "$container_sum")" \
        --argjson containerCount "$(num_or_null "$container_count")" \
        --argjson containers "$container_json" \
        --argjson usage "$(num_or_null "$u")" \
        --argjson user "$(num_or_null "$us")" \
        --argjson system "$(num_or_null "$sy")" \
        --argjson nrThrottled "$(num_or_null "$nt")" \
        --argjson throttled "$(num_or_null "$tu")" \
        --arg cgroupPath "$cg" \
        --arg procPattern "$PROC_PATTERN" \
        --argjson interval "$interval" \
        '{
            timestamp: $ts,
            epochSeconds: $epoch,
            intervalSeconds: $interval,
            memCurrentBytes: $memCurrent,
            memAvailableBytes: $memAvailable,
            rssSumBytes: $rssSum,
            rssProcessCount: $rssCount,
            containerSumBytes: $containerSum,
            containerCount: $containerCount,
            containers: $containers,
            cpu: {
                usageUsec: $usage,
                userUsec: $user,
                systemUsec: $system,
                nrThrottled: $nrThrottled,
                throttledUsec: $throttled
            },
            cgroupPath: $cgroupPath,
            procPattern: $procPattern
        }' >>"$out_dir/samples.jsonl" 2>/dev/null
}

cmd_sample() {
    local out_dir="" interval="5" tick=0 start_epoch page cg
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --out-dir)
                out_dir="$2"
                shift 2
                ;;
            --interval)
                interval="$2"
                shift 2
                ;;
            *) shift ;;
        esac
    done
    [ -n "$out_dir" ] || exit 2
    trap 'exit 0' TERM INT
    printf '%s\n' "$$" >"$out_dir/sampler.pid" 2>/dev/null
    cg="$(self_cgroup_path)"
    page="$(getconf PAGESIZE 2>/dev/null)" || page=""
    case "$page" in
        '' | *[!0-9]*) page=4096 ;;
    esac
    start_epoch="$(now_epoch_float)"
    while :; do
        [ -d "$out_dir" ] || exit 0
        tick=$((tick + 1))
        sample_once "$out_dir" "$interval" "$cg" "$page" || true
        if [ "$SAMPLE_CAP" -lt 100 ]; then
            cap_samples "$out_dir/samples.jsonl"
        elif [ $((tick % 50)) -eq 0 ]; then
            cap_samples "$out_dir/samples.jsonl"
        fi
        now="$(now_epoch_float)"
        if awk -v a="$start_epoch" -v b="$now" -v max="$SAMPLER_MAX_LIFETIME" \
            'BEGIN { exit !((b - a) > max) }'; then
            exit 0
        fi
        sleep "$interval" 2>/dev/null || sleep 5
    done
}

################################################################################
# log parsing (single streaming pass)
################################################################################

parse_log_facts() {
    # $1 = gradle log file. Emits TSV records:
    #   KV <name> <value>
    #   STEP <isoTs> <bucket> <command>   (command is raw here; redacted before storage)
    #   LASTTS <isoTs>
    awk '
        function classify(cmd,   c) {
            c = tolower(cmd)
            if (index(c, "gradlew") > 0 && (index(c, "setup") > 0 || index(c, "generatecertificates") > 0)) return "setup"
            if (index(c, "gradlew") > 0 && (index(c, "compilejava") > 0 || index(c, "assemble") > 0 || index(c, "build") > 0)) return "compile"
            if (index(c, "runstartupcheck") > 0) return "readiness"
            if (index(c, "runcontainer") > 0 || index(c, "runha") > 0) return "test"
            if (index(c, "run") > 0 && index(c, "test") > 0) return "test"
            if (index(c, "coverage") > 0 || index(c, "sonar") > 0 || index(c, "upload-artifact") > 0) return "report"
            return "other"
        }
        {
            if (index($0, "FROM-CACHE") > 0) fromCache++
            if (index($0, "Could not load entry") > 0) failedReads++
            if (index($0, "The remote build cache was disabled during the build due to errors") > 0) disabled = 1
            if ($0 ~ /Download https?:/) downloads++
            if (index($0, "OutOfMemoryError") > 0) oomLog++
            if (index($0, "Killed process") > 0) oomLog++

            if (index($0, "Test") > 0 && $0 ~ /(PASSED|FAILED|SKIPPED)[[:space:]]*$/) {
                if ($0 ~ /PASSED[[:space:]]*$/) passed++
                else if ($0 ~ /FAILED[[:space:]]*$/) failed++
                else skipped++
            }
            if (index($0, "BUILD SUCCESSFUL") > 0) buildOK++
            if (index($0, "BUILD FAILED") > 0) buildFail++

            if ($0 ~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T/) lastts = $1

            if (index($0, "##[group]Run ") > 0) {
                ts = $1
                if (ts ~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T/) {
                    idx = index($0, "##[group]Run ")
                    cmd = substr($0, idx + 13)
                    gsub(/\t/, " ", cmd)
                    printf "STEP\t%s\t%s\t%s\n", ts, classify(cmd), cmd
                }
            }
        }
        END {
            printf "KV\tfromCacheTasks\t%d\n", fromCache + 0
            printf "KV\tfailedReads\t%d\n", failedReads + 0
            printf "KV\tdisabledDuringBuild\t%d\n", disabled + 0
            printf "KV\tdependencyDownloads\t%d\n", downloads + 0
            printf "KV\ttestsPassed\t%d\n", passed + 0
            printf "KV\ttestsFailed\t%d\n", failed + 0
            printf "KV\ttestsSkipped\t%d\n", skipped + 0
            printf "KV\tbuildSuccessful\t%d\n", buildOK + 0
            printf "KV\tbuildFailed\t%d\n", buildFail + 0
            printf "KV\toomLogLines\t%d\n", oomLog + 0
            printf "LASTTS\t%s\n", lastts
        }
    ' "$1" 2>/dev/null
}

iso_to_epoch() {
    # $1 = ISO8601 UTC timestamp with optional fraction and Z. prints epoch seconds or nothing
    local ts="$1" base v
    ts="${ts%Z}"
    base="${ts%%.*}"
    case "$base" in
        ????-??-??T??:??:??) : ;;
        *) return 1 ;;
    esac
    v="$(date -u -d "${base}Z" +%s 2>/dev/null)" || v=""
    if [ -z "$v" ] && date -u -j -f "%Y-%m-%dT%H:%M:%S" "${base}" +%s >/dev/null 2>&1; then
        v="$(date -u -j -f "%Y-%m-%dT%H:%M:%S" "${base}" +%s 2>/dev/null)" || v=""
    fi
    case "$v" in
        '' | *[!0-9]*) return 1 ;;
    esac
    printf '%s' "$v"
}

################################################################################
# state / aggregation
################################################################################

state_get() {
    # $1 = state file, $2 = jq path, $3 = default
    local f="$1" path="$2" def="${3:-}"
    [ -f "$f" ] || {
        printf '%s' "$def"
        return 0
    }
    local v
    v="$(jq -r "$path // empty" "$f" 2>/dev/null)" || v=""
    [ -n "$v" ] || v="$def"
    printf '%s' "$v"
}

kill_sampler() {
    # $1 = out dir. Returns 0 always; prints a warning when the sampler was already gone.
    local out_dir="$1" pid="" i
    if [ -f "$out_dir/state.json" ]; then
        pid="$(state_get "$out_dir/state.json" '.samplerPid' '')"
    fi
    if [ -z "$pid" ] && [ -f "$out_dir/sampler.pid" ]; then
        pid="$(head -n 1 "$out_dir/sampler.pid" 2>/dev/null)" || pid=""
    fi
    case "$pid" in
        '' | *[!0-9]*)
            warn "sampler pid unavailable (cancellation path: sampler already gone)"
            return 0
            ;;
    esac
    if kill -0 "$pid" 2>/dev/null; then
        kill -TERM "$pid" 2>/dev/null || true
        for i in 1 2 3 4 5 6; do
            kill -0 "$pid" 2>/dev/null || break
            sleep 0.5
        done
        if kill -0 "$pid" 2>/dev/null; then
            kill -KILL "$pid" 2>/dev/null || true
        fi
    else
        warn "sampler pid $pid is no longer running (cancellation path)"
    fi
    rm -f "$out_dir/sampler.pid" 2>/dev/null || true
    return 0
}

aggregate_samples() {
    # $1 = samples.jsonl. Prints TSV:
    #   count rssPeak containerPeak tickPeak memCurrentPeak usage user system nrThrottled throttled
    local f="$1"
    if [ ! -s "$f" ]; then
        printf '0\t0\t0\t0\t0\t\t\t\t\t\n'
        return 0
    fi
    jq -r '[
            (.rssSumBytes // 0), (.containerSumBytes // 0), (.memCurrentBytes // 0),
            (.cpu.usageUsec // ""), (.cpu.userUsec // ""), (.cpu.systemUsec // ""),
            (.cpu.nrThrottled // ""), (.cpu.throttledUsec // "")
        ] | @tsv' "$f" 2>/dev/null | awk -F'\t' '
        {
            n++
            if ($1 > rss) rss = $1
            if ($2 > cont) cont = $2
            if ($3 > cur) cur = $3
            t = ($1 > $2 ? $1 : $2)
            if (t > tick) tick = t
            if ($4 != "") { u = $4; us = $5; sy = $6; nt = $7; tu = $8 }
        }
        END {
            printf "%d\t%d\t%d\t%d\t%d\t%s\t%s\t%s\t%s\t%s\n", n + 0, rss + 0, cont + 0, tick + 0, cur + 0, u, us, sy, nt, tu
        }'
}

count_samples() {
    local f="$1" n
    [ -f "$f" ] || {
        printf '0'
        return 0
    }
    n="$(wc -l <"$f" 2>/dev/null)" || n=""
    case "$n" in
        '' | *[!0-9]*) printf '0' ;;
        *) printf '%s' "$n" ;;
    esac
}

oom_events() {
    # prints "<events or empty>"
    local raw v
    raw="$(read_cgroup_file memory.events)" || raw=""
    [ -n "$raw" ] || return 1
    v="$(printf '%s\n' "$raw" | awk '
        $1 == "oom"      { o = $2 }
        $1 == "oom_kill" { k = $2 }
        END { if (o == "" && k == "") exit 1; printf "%d", (o + 0) + (k + 0) }
    ')"
    [ -n "$v" ] || return 1
    printf '%s' "$v"
}

################################################################################
# emit
################################################################################

EMIT_WARNINGS=()
emit_warn() {
    EMIT_WARNINGS+=("$1")
    warn "$1"
}

write_summary_block() {
    # $1 = out dir, $2 = summary markdown text
    local out_dir="$1" text="$2" target="${GITHUB_STEP_SUMMARY:-}"
    [ -n "$target" ] || return 0
    if [ ! -e "$target" ]; then
        : >"$target" 2>/dev/null || {
            warn "GITHUB_STEP_SUMMARY is not creatable; skipping step summary"
            return 0
        }
    fi
    [ -w "$target" ] || {
        warn "GITHUB_STEP_SUMMARY is not writable; skipping step summary"
        return 0
    }
    printf '%s' "$text" | redact_text >>"$target" 2>/dev/null || warn "could not append to GITHUB_STEP_SUMMARY"
    return 0
}

write_csv_row() {
    # $1 = out dir, $2 = already-joined CSV row (no header)
    local out_dir="$1" row="$2"
    local f="$out_dir/resources.csv"
    local header='label,sha,workflow,job,jdk,cpuCount,memTotalBytes,durationTotalSeconds,peakMemoryBytes,peakSource,cpuUsageUsec,nrThrottled,gradleFromCache,gradleFailedReads,gradleCacheDisabled,testsExecuted,testsFailed,result'
    if [ ! -s "$f" ]; then
        printf '%s\n' "$header" >"$f" 2>/dev/null || {
            warn "could not write $f"
            return 0
        }
    fi
    printf '%s\n' "$row" >>"$f" 2>/dev/null || warn "could not append to $f"
    return 0
}

csv_field() {
    local v="${1:-}"
    case "$v" in
        null) printf '' ;;
        *[,\"]* | *'
'*)
            v="${v//\"/\"\"}"
            printf '"%s"' "$v"
            ;;
        *) printf '%s' "$v" ;;
    esac
}

emit_resources() {
    # Emits resources.json / resources.csv / step summary.
    # Required vars: OUT_DIR, LABEL, RUN_*, RUNNER_*, JDK_*, ENV_*, DUR_*, MEM_*, CPU_*,
    #                DISK_*, OOM_*, GC_*, TESTS_*, SAMPLE_*
    local out_dir="$OUT_DIR" json="$OUT_DIR/resources.json" generated_at
    generated_at="$(iso_utc)"

    jq -cn \
        --argjson schemaVersion 1 \
        --arg generatedAt "$generated_at" \
        --arg runId "${RUN_ID:-unknown}" \
        --arg runAttempt "${RUN_ATTEMPT:-unknown}" \
        --arg workflow "${RUN_WORKFLOW:-unknown}" \
        --arg job "${RUN_JOB:-unknown}" \
        --arg event "${RUN_EVENT:-unknown}" \
        --arg refName "${RUN_REF:-unknown}" \
        --arg sha "${RUN_SHA:-unknown}" \
        --arg label "${LABEL:-unknown}" \
        --arg os "${RUNNER_OS:-unknown}" \
        --arg arch "${RUNNER_ARCH:-unknown}" \
        --argjson cpuCount "$(num_or_null "${RUNNER_CPU_COUNT:-}")" \
        --argjson memTotal "$(num_or_null "${RUNNER_MEM_TOTAL:-}")" \
        --argjson memAvailable "$(num_or_null "${RUNNER_MEM_AVAILABLE:-}")" \
        --argjson jdkVersion "$(str_or_null "${JDK_VERSION:-}")" \
        --argjson jdkVendor "$(str_or_null "${JDK_VENDOR:-}")" \
        --argjson jdkHome "$(str_or_null "${JDK_HOME:-}")" \
        --arg topology "${ENV_TOPOLOGY:-unknown}" \
        --arg cacheState "${ENV_CACHE_STATE:-unknown}" \
        --argjson dockerContainers "$(num_or_null "${ENV_DOCKER_CONTAINERS:-}")" \
        --argjson durationTotal "$(num_or_null "${DUR_TOTAL:-}")" \
        --argjson durationSetup "$(num_or_null "${DUR_SETUP:-}")" \
        --arg phasesSource "${DUR_PHASES_SOURCE:-unavailable}" \
        --argjson durationCompile "$(num_or_null "${DUR_COMPILE:-}")" \
        --argjson durationReadiness "$(num_or_null "${DUR_READINESS:-}")" \
        --argjson durationTest "$(num_or_null "${DUR_TEST:-}")" \
        --argjson durationReport "$(num_or_null "${DUR_REPORT:-}")" \
        --argjson peakSampled "$(num_or_null "${MEM_PEAK_SAMPLED:-}")" \
        --arg peakSource "${MEM_PEAK_SOURCE:-unavailable}" \
        --argjson peakIsLowerBound "${MEM_PEAK_LOWER_BOUND:-true}" \
        --argjson cgroupPeak "$(num_or_null "${MEM_CGROUP_PEAK:-}")" \
        --argjson containerPeakSum "$(num_or_null "${MEM_CONTAINER_PEAK:-}")" \
        --argjson sampledSumPeak "$(num_or_null "${MEM_SAMPLED_PEAK:-}")" \
        --argjson cpuUsage "$(num_or_null "${CPU_USAGE:-}")" \
        --argjson cpuUser "$(num_or_null "${CPU_USER:-}")" \
        --argjson cpuSystem "$(num_or_null "${CPU_SYSTEM:-}")" \
        --argjson cpuNrThrottled "$(num_or_null "${CPU_NR_THROTTLED:-}")" \
        --argjson cpuThrottled "$(num_or_null "${CPU_THROTTLED:-}")" \
        --argjson workspaceBytes "$(num_or_null "${DISK_WORKSPACE:-}")" \
        --argjson gradleCacheBytes "$(num_or_null "${DISK_GRADLE_CACHE:-}")" \
        --argjson oomEvents "$(num_or_null "${OOM_EVENTS:-}")" \
        --argjson oomEvidence "$(json_array ${OOM_EVIDENCE[@]+"${OOM_EVIDENCE[@]}"})" \
        --argjson gcFromCache "$(num_or_null "${GC_FROM_CACHE:-}")" \
        --argjson gcFailedReads "$(num_or_null "${GC_FAILED_READS:-}")" \
        --argjson gcDisabled "${GC_DISABLED:-null}" \
        --argjson gcDownloads "$(num_or_null "${GC_DOWNLOADS:-}")" \
        --argjson testsSelected "$(num_or_null "${TESTS_SELECTED:-}")" \
        --argjson testsDiscovered "$(num_or_null "${TESTS_DISCOVERED:-}")" \
        --argjson testsExecuted "$(num_or_null "${TESTS_EXECUTED:-}")" \
        --argjson testsSkipped "$(num_or_null "${TESTS_SKIPPED:-}")" \
        --argjson testsFailed "$(num_or_null "${TESTS_FAILED:-}")" \
        --arg testsResult "${TESTS_RESULT:-unknown}" \
        --argjson sampleCount "$(num_or_null "${SAMPLE_COUNT:-}")" \
        --argjson sampleInterval "$(num_or_null "${SAMPLE_INTERVAL:-}")" \
        --argjson samplesPath "$(str_or_null "${SAMPLE_PATH:-}")" \
        --argjson samplesTruncated "${SAMPLE_TRUNCATED:-false}" \
        '{
            schemaVersion: $schemaVersion,
            generatedAt: $generatedAt,
            run: {
                id: $runId,
                attempt: $runAttempt,
                workflow: $workflow,
                job: $job,
                event: $event,
                ref: $refName,
                sha: $sha,
                label: $label
            },
            runner: {
                os: $os,
                arch: $arch,
                cpuCount: $cpuCount,
                memTotalBytes: $memTotal,
                memAvailableBytes: $memAvailable
            },
            jdk: { version: $jdkVersion, vendor: $jdkVendor, home: $jdkHome },
            environment: {
                topology: $topology,
                cacheState: $cacheState,
                dockerContainers: $dockerContainers
            },
            durationSeconds: {
                total: $durationTotal,
                setup: $durationSetup,
                phasesSource: $phasesSource,
                compile: $durationCompile,
                readiness: $durationReadiness,
                test: $durationTest,
                report: $durationReport
            },
            memoryBytes: {
                peakSampled: $peakSampled,
                peakSource: $peakSource,
                peakIsLowerBound: $peakIsLowerBound,
                cgroupPeak: $cgroupPeak,
                containerPeakSum: $containerPeakSum,
                sampledSumPeak: $sampledSumPeak
            },
            cpu: {
                usageUsec: $cpuUsage,
                userUsec: $cpuUser,
                systemUsec: $cpuSystem,
                nrThrottled: $cpuNrThrottled,
                throttledUsec: $cpuThrottled
            },
            disk: { workspaceBytes: $workspaceBytes, gradleCacheBytes: $gradleCacheBytes },
            oom: { events: $oomEvents, evidence: $oomEvidence },
            gradleCache: {
                fromCacheTasks: $gcFromCache,
                failedReads: $gcFailedReads,
                disabledDuringBuild: $gcDisabled,
                dependencyDownloads: $gcDownloads
            },
            tests: {
                selected: $testsSelected,
                discovered: $testsDiscovered,
                executed: $testsExecuted,
                skipped: $testsSkipped,
                failed: $testsFailed,
                result: $testsResult
            },
            samples: {
                count: $sampleCount,
                intervalSeconds: $sampleInterval,
                path: $samplesPath,
                truncated: $samplesTruncated
            }
        }' >"$json" 2>/dev/null || {
        warn "failed to build $json with jq"
        return 1
    }

    redact_file "$json"

    local size
    size="$(wc -c <"$json" 2>/dev/null)" || size=""
    case "$size" in
        '' | *[!0-9]*) : ;;
        *) [ "$size" -le 20480 ] || warn "resources.json is ${size} bytes (target < 20480)" ;;
    esac

    local row
    row="$(printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s' \
        "$(csv_field "${LABEL:-unknown}")" \
        "$(csv_field "${RUN_SHA:-unknown}")" \
        "$(csv_field "${RUN_WORKFLOW:-unknown}")" \
        "$(csv_field "${RUN_JOB:-unknown}")" \
        "$(csv_field "${JDK_VERSION:-null}")" \
        "$(csv_field "${RUNNER_CPU_COUNT:-null}")" \
        "$(csv_field "${RUNNER_MEM_TOTAL:-null}")" \
        "$(csv_field "${DUR_TOTAL:-}")" \
        "$(csv_field "${MEM_PEAK_SAMPLED:-null}")" \
        "$(csv_field "${MEM_PEAK_SOURCE:-unavailable}")" \
        "$(csv_field "${CPU_USAGE:-null}")" \
        "$(csv_field "${CPU_NR_THROTTLED:-null}")" \
        "$(csv_field "${GC_FROM_CACHE:-null}")" \
        "$(csv_field "${GC_FAILED_READS:-null}")" \
        "$(csv_field "${GC_DISABLED:-null}")" \
        "$(csv_field "${TESTS_EXECUTED:-null}")" \
        "$(csv_field "${TESTS_FAILED:-null}")" \
        "$(csv_field "${TESTS_RESULT:-unknown}")")"
    write_csv_row "$out_dir" "$row"

    local text
    text="$(printf '%s\n' \
        "### CI resource summary - ${LABEL:-unknown}" \
        "" \
        "- run: ${RUN_WORKFLOW:-unknown} #${RUN_ID:-unknown} attempt ${RUN_ATTEMPT:-unknown} (${RUN_EVENT:-unknown}, ${RUN_REF:-unknown})" \
        "- job: ${RUN_JOB:-unknown} on ${RUNNER_OS:-unknown}/${RUNNER_ARCH:-unknown}, ${RUNNER_CPU_COUNT:-unknown} cpu, ${RUNNER_MEM_TOTAL:-unknown} bytes RAM" \
        "- jdk: ${JDK_VERSION:-null} (${JDK_VENDOR:-null})" \
        "- topology/cache: ${ENV_TOPOLOGY:-unknown} / ${ENV_CACHE_STATE:-unknown}" \
        "- duration: total ${DUR_TOTAL:-unavailable}s, setup ${DUR_SETUP:-null}, compile ${DUR_COMPILE:-null}, readiness ${DUR_READINESS:-null}, test ${DUR_TEST:-null}, report ${DUR_REPORT:-null} (source: ${DUR_PHASES_SOURCE:-unavailable})" \
        "- memory: peak ${MEM_PEAK_SAMPLED:-null} bytes (source: ${MEM_PEAK_SOURCE:-unavailable}, lowerBound: ${MEM_PEAK_LOWER_BOUND:-true})" \
        "- cpu: usage ${CPU_USAGE:-null} usec, throttled ${CPU_NR_THROTTLED:-null} events" \
        "- gradle cache: fromCache ${GC_FROM_CACHE:-null}, failedReads ${GC_FAILED_READS:-null}, disabled ${GC_DISABLED:-null}, dependencyDownloads ${GC_DOWNLOADS:-null}" \
        "- tests: executed ${TESTS_EXECUTED:-null}, skipped ${TESTS_SKIPPED:-null}, failed ${TESTS_FAILED:-null}, result ${TESTS_RESULT:-unknown}" \
        "- samples: ${SAMPLE_COUNT:-0} at interval ${SAMPLE_INTERVAL:-unknown}s (${SAMPLE_PATH:-null}, truncated: ${SAMPLE_TRUNCATED:-false})")"

    if [ "$(array_count PHASE_DETAIL)" -gt 0 ]; then
        local d
        text="${text}
- phases:"
        for d in ${PHASE_DETAIL[@]+"${PHASE_DETAIL[@]}"}; do
            text="${text}
  - ${d}"
        done
    fi
    if [ "$(array_count EMIT_WARNINGS)" -gt 0 ]; then
        local w
        text="${text}
- warnings:"
        for w in ${EMIT_WARNINGS[@]+"${EMIT_WARNINGS[@]}"}; do
            text="${text}
  - ${w}"
        done
    fi
    write_summary_block "$out_dir" "${text}
"
    redact_file "$json"
    return 0
}

################################################################################
# phase helpers
################################################################################

PHASE_DETAIL=()

load_log_facts() {
    # $1 = log file. Sets GC_*, TESTS_*, OOM log lines and phase durations.
    local log="$1" kind a b c
    local -a step_ts=() step_bucket=() step_cmd=()
    local last_ts=""
    local passed=0 failed=0 skipped=0 buildOK=0 buildFail=0 oomLines=0

    while IFS=$'\t' read -r kind a b c; do
        case "$kind" in
            KV)
                case "$a" in
                    fromCacheTasks) GC_FROM_CACHE="$b" ;;
                    failedReads) GC_FAILED_READS="$b" ;;
                    disabledDuringBuild) [ "$b" = "1" ] && GC_DISABLED=true || GC_DISABLED=false ;;
                    dependencyDownloads) GC_DOWNLOADS="$b" ;;
                    testsPassed) passed="$b" ;;
                    testsFailed) failed="$b" ;;
                    testsSkipped) skipped="$b" ;;
                    buildSuccessful) buildOK="$b" ;;
                    buildFailed) buildFail="$b" ;;
                    oomLogLines) oomLines="$b" ;;
                esac
                ;;
            STEP)
                step_ts+=("$a")
                step_bucket+=("$b")
                step_cmd+=("$c")
                ;;
            LASTTS)
                last_ts="$a"
                ;;
        esac
    done < <(parse_log_facts "$log")

    TESTS_EXECUTED="$((passed + failed))"
    TESTS_SKIPPED="$skipped"
    TESTS_FAILED="$failed"
    if [ "$buildOK" -gt 0 ] && [ "$buildFail" -eq 0 ]; then
        TESTS_RESULT="success"
    elif [ "$buildFail" -gt 0 ]; then
        TESTS_RESULT="failure"
    else
        TESTS_RESULT="unknown"
    fi
    if [ "$oomLines" -gt 0 ]; then
        OOM_EVIDENCE+=("log: ${oomLines} OutOfMemoryError/Killed-process lines")
        if [ -z "${OOM_EVENTS:-}" ]; then
            OOM_EVENTS="$oomLines"
        fi
    fi

    local n="${#step_ts[@]}" i start end bucket delta cmd redacted
    if [ "$n" -eq 0 ]; then
        return 1
    fi
    local parsed=0
    for i in $(seq 0 $((n - 1))); do
        start="$(iso_to_epoch "${step_ts[$i]}")" || continue
        if [ "$i" -lt "$((n - 1))" ]; then
            end="$(iso_to_epoch "${step_ts[$((i + 1))]}")" || continue
        else
            end="$(iso_to_epoch "$last_ts")" || continue
        fi
        bucket="${step_bucket[$i]}"
        delta=$((end - start))
        [ "$delta" -ge 0 ] || delta=0
        parsed=$((parsed + 1))
        case "$bucket" in
            setup) DUR_SETUP=$((DUR_SETUP + delta)) ;;
            compile) DUR_COMPILE=$((DUR_COMPILE + delta)) ;;
            readiness) DUR_READINESS=$((DUR_READINESS + delta)) ;;
            test) DUR_TEST=$((DUR_TEST + delta)) ;;
            report) DUR_REPORT=$((DUR_REPORT + delta)) ;;
            *) continue ;;
        esac
        redacted="$(printf '%s' "${step_cmd[$i]}" | redact_text)"
        if [ "${#redacted}" -gt 120 ]; then
            redacted="${redacted:0:117}..."
        fi
        PHASE_DETAIL+=("${bucket}: ${delta}s (${redacted})")
    done
    [ "$parsed" -gt 0 ] || return 1
    DUR_PHASES_SOURCE="log"
    return 0
}

load_marks() {
    # $1 = out dir, $2 = start epoch (float). Sets phase durations from marks.
    local out_dir="$1" start="$2" marks="$1/marks.jsonl" prev name epoch delta bucket
    [ -s "$marks" ] || return 1
    prev="$start"
    while IFS=$'\t' read -r name epoch; do
        [ -n "$name" ] || continue
        case "$epoch" in
            '' | *[!0-9.]*) continue ;;
        esac
        delta="$(awk -v a="$prev" -v b="$epoch" 'BEGIN { d = b - a; if (d < 0) d = 0; printf "%d", d }')"
        case "$name" in
            setup) DUR_SETUP=$((DUR_SETUP + delta)) ;;
            compile) DUR_COMPILE=$((DUR_COMPILE + delta)) ;;
            readiness) DUR_READINESS=$((DUR_READINESS + delta)) ;;
            test) DUR_TEST=$((DUR_TEST + delta)) ;;
            report) DUR_REPORT=$((DUR_REPORT + delta)) ;;
            *) prev="$epoch"; continue ;;
        esac
        prev="$epoch"
    done < <(jq -r '[.name, .epochSeconds] | @tsv' "$marks" 2>/dev/null)
    return 0
}

derive_cache_state() {
    local override="${CI_RESOURCE_CACHE_STATE:-}"
    case "$override" in
        cold | warm)
            printf '%s' "$override"
            return 0
            ;;
    esac
    if [ -n "${GC_FROM_CACHE:-}" ] && [ "${GC_FROM_CACHE:-0}" -gt 0 ]; then
        printf 'warm'
        return 0
    fi
    if [ -n "${GC_DOWNLOADS:-}" ] && [ "${GC_DOWNLOADS:-0}" -gt 0 ]; then
        printf 'cold'
        return 0
    fi
    if [ -n "${GC_FROM_CACHE:-}" ] && [ "${GC_DOWNLOADS:-}" = "0" ] && [ "${GC_FAILED_READS:-}" = "0" ]; then
        printf 'unknown'
        return 0
    fi
    printf 'unknown'
}

reset_metrics() {
    RUN_ID="${GITHUB_RUN_ID:-unknown}"
    RUN_ATTEMPT="${GITHUB_RUN_ATTEMPT:-unknown}"
    RUN_WORKFLOW="${GITHUB_WORKFLOW:-unknown}"
    RUN_JOB="${GITHUB_JOB:-unknown}"
    RUN_EVENT="${GITHUB_EVENT_NAME:-unknown}"
    RUN_REF="${GITHUB_REF_NAME:-unknown}"
    RUN_SHA="${GITHUB_SHA:-unknown}"

    RUNNER_OS="$(uname -s 2>/dev/null)"
    RUNNER_ARCH="$(uname -m 2>/dev/null)"
    [ -n "$RUNNER_OS" ] || RUNNER_OS="unknown"
    [ -n "$RUNNER_ARCH" ] || RUNNER_ARCH="unknown"
    RUNNER_CPU_COUNT="$(probe_cpu_count)"
    RUNNER_MEM_TOTAL="$(probe_mem_total_bytes)"
    RUNNER_MEM_AVAILABLE="$(probe_mem_available_bytes)"

    JDK_VERSION="$(probe_jdk_version)"
    JDK_VENDOR="$(probe_jdk_vendor)"
    JDK_HOME="${JAVA_HOME:-null}"

    ENV_TOPOLOGY="${ENV_CONFIG:-unknown}"
    ENV_DOCKER_CONTAINERS="$(probe_container_count)"

    DUR_TOTAL=""
    DUR_SETUP=""
    DUR_COMPILE=""
    DUR_READINESS=""
    DUR_TEST=""
    DUR_REPORT=""
    DUR_PHASES_SOURCE="unavailable"

    MEM_PEAK_SAMPLED=""
    MEM_PEAK_SOURCE="unavailable"
    MEM_PEAK_LOWER_BOUND="true"
    MEM_CGROUP_PEAK=""
    MEM_CONTAINER_PEAK=""
    MEM_SAMPLED_PEAK=""

    CPU_USAGE=""
    CPU_USER=""
    CPU_SYSTEM=""
    CPU_NR_THROTTLED=""
    CPU_THROTTLED=""

    DISK_WORKSPACE=""
    DISK_GRADLE_CACHE=""

    OOM_EVENTS=""
    OOM_EVIDENCE=()

    GC_FROM_CACHE=""
    GC_FAILED_READS=""
    GC_DISABLED="null"
    GC_DOWNLOADS=""

    TESTS_SELECTED=""
    TESTS_DISCOVERED=""
    TESTS_EXECUTED=""
    TESTS_SKIPPED=""
    TESTS_FAILED=""
    TESTS_RESULT="unknown"

    SAMPLE_COUNT="0"
    SAMPLE_INTERVAL=""
    SAMPLE_PATH="null"
    SAMPLE_TRUNCATED="false"

    PHASE_DETAIL=()
    EMIT_WARNINGS=()
}

fill_disk_and_oom() {
    local ws="${GITHUB_WORKSPACE:-$(pwd)}"
    DISK_WORKSPACE="$(dir_bytes "$ws")" || DISK_WORKSPACE=""
    if [ -z "$DISK_WORKSPACE" ]; then
        warn "disk.workspaceBytes unavailable for $ws"
    fi
    local gc_dir
    gc_dir="$(gradle_cache_dir)"
    DISK_GRADLE_CACHE="$(dir_bytes "$gc_dir")" || DISK_GRADLE_CACHE=""
    if [ -z "$DISK_GRADLE_CACHE" ]; then
        warn "disk.gradleCacheBytes unavailable for ${gc_dir:-<unset>}"
    fi

    local ev
    ev="$(oom_events)" || ev=""
    if [ -n "$ev" ]; then
        OOM_EVENTS="$ev"
        OOM_EVIDENCE+=("cgroup memory.events oom+oom_kill=${ev}")
    else
        warn "oom.events unavailable (cgroup memory.events not readable)"
    fi
}

################################################################################
# commands
################################################################################

cmd_start() {
    local label="" out_dir="" interval="5" out
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --label)
                label="${2:-}"
                shift 2
                ;;
            --out-dir)
                out_dir="${2:-}"
                shift 2
                ;;
            --interval)
                interval="${2:-}"
                shift 2
                ;;
            *) die "start: unexpected argument '$1'" ;;
        esac
    done
    [ -n "$label" ] || die "start: --label is required"
    case "$interval" in
        '' | *[!0-9.]*) die "start: --interval must be a number" ;;
    esac
    out="$(resolve_out_dir "$out_dir")"
    mkdir -p "$out" || die "cannot create out-dir $out"

    # a previous sampler for this out-dir would double-sample; retire it
    if [ -f "$out/state.json" ]; then
        kill_sampler "$out"
    fi

    local start_epoch cg
    start_epoch="$(now_epoch_float)"
    cg="$(self_cgroup_path)"

    jq -cn \
        --arg label "$label" \
        --arg startedAt "$(iso_utc)" \
        --argjson startEpoch "$(num_or_null "$start_epoch")" \
        --argjson interval "$(num_or_null "$interval")" \
        --arg outDir "$out" \
        --arg cgroupPath "$cg" \
        --arg procPattern "$PROC_PATTERN" \
        --argjson sampleCap "$SAMPLE_CAP" \
        --arg runnerTemp "${RUNNER_TEMP:-null}" \
        '{ label: $label, startedAt: $startedAt, startEpoch: $startEpoch, interval: $interval,
           outDir: $outDir, cgroupPath: $cgroupPath, procPattern: $procPattern,
           sampleCap: $sampleCap, runnerTemp: $runnerTemp }' >"$out/state.json" 2>/dev/null \
        || die "cannot write $out/state.json"

    : >"$out/samples.jsonl"

    nohup "$SELF_PATH" _sample --out-dir "$out" --interval "$interval" >/dev/null 2>&1 &
    local pid=$!
    disown 2>/dev/null || true

    local i pfile="$out/sampler.pid"
    for i in 1 2 3 4 5 6 7 8 9 10; do
        [ -s "$pfile" ] && break
        sleep 0.1
    done
    if [ -s "$pfile" ]; then
        pid="$(head -n 1 "$pfile" 2>/dev/null)" || pid=""
    fi
    case "$pid" in
        '' | *[!0-9]*) warn "sampler pid could not be recorded" ;;
        *)
            jq -c --argjson pid "$pid" '. + { samplerPid: $pid }' "$out/state.json" >"$out/state.json.tmp" 2>/dev/null \
                && mv -f "$out/state.json.tmp" "$out/state.json" 2>/dev/null
            ;;
    esac

    info "$out"
}

cmd_mark() {
    local name="" out_dir="" out epoch
    if [ "$#" -gt 0 ]; then
        case "$1" in
            --*)
                : ;;
            *)
                name="$1"
                shift
                ;;
        esac
    fi
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --out-dir)
                out_dir="${2:-}"
                shift 2
                ;;
            *) shift ;;
        esac
    done
    [ -n "$name" ] || die "mark: phase name is required"
    out="$(resolve_out_dir "$out_dir")"
    mkdir -p "$out" || die "cannot create out-dir $out"
    epoch="$(now_epoch_float)"
    jq -cn --arg name "$name" --arg ts "$(iso_utc)" --argjson epoch "$(num_or_null "$epoch")" \
        '{ name: $name, timestamp: $ts, epochSeconds: $epoch }' >>"$out/marks.jsonl" 2>/dev/null \
        || die "cannot write $out/marks.jsonl"
    info "marked $name"
}

cmd_stop() {
    local label="" out_dir="" log="" out
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --label)
                label="${2:-}"
                shift 2
                ;;
            --out-dir)
                out_dir="${2:-}"
                shift 2
                ;;
            --log)
                log="${2:-}"
                shift 2
                ;;
            *) die "stop: unexpected argument '$1'" ;;
        esac
    done
    out="$(resolve_out_dir "$out_dir")"
    mkdir -p "$out" 2>/dev/null || warn "cannot create out-dir $out"
    OUT_DIR="$out"

    reset_metrics

    local state="$out/state.json" start_epoch="" interval="" trunc_flag="false"
    if [ -f "$state" ]; then
        label="${label:-$(state_get "$state" '.label' '')}"
        start_epoch="$(state_get "$state" '.startEpoch' '')"
        interval="$(state_get "$state" '.interval' '')"
    else
        warn "no state file at $state (stop without start, or state lost)"
    fi
    [ -n "$label" ] || label="unknown"
    LABEL="$label"

    kill_sampler "$out"

    local samples="$out/samples.jsonl"
    if [ -s "$samples" ]; then
        cap_samples "$samples"
        SAMPLE_COUNT="$(count_samples "$samples")"
        SAMPLE_PATH="samples.jsonl"
        [ -f "${samples}.truncated" ] && trunc_flag="true"
        local agg
        agg="$(aggregate_samples "$samples")"
        MEM_SAMPLED_PEAK="$(printf '%s' "$agg" | cut -f2)"
        MEM_CONTAINER_PEAK="$(printf '%s' "$agg" | cut -f3)"
        CPU_USAGE="$(printf '%s' "$agg" | cut -f6)"
        CPU_USER="$(printf '%s' "$agg" | cut -f7)"
        CPU_SYSTEM="$(printf '%s' "$agg" | cut -f8)"
        CPU_NR_THROTTLED="$(printf '%s' "$agg" | cut -f9)"
        CPU_THROTTLED="$(printf '%s' "$agg" | cut -f10)"
        local tick_peak
        tick_peak="$(printf '%s' "$agg" | cut -f4)"
        if [ -n "$tick_peak" ] && [ "$tick_peak" != "0" ]; then
            MEM_PEAK_SAMPLED="$tick_peak"
            MEM_PEAK_SOURCE="sampled-sum"
            MEM_PEAK_LOWER_BOUND="true"
        fi
        if [ -z "${CPU_USAGE:-}" ]; then
            warn "cpu.stat counters unavailable in samples (cgroup cpu.stat not readable)"
        fi
    else
        warn "no samples collected (samples.jsonl missing or empty)"
        [ -f "${samples}.truncated" ] && trunc_flag="true"
    fi

    # The cgroup memory.peak counter is only attributable to this run when the run was
    # actually observed. Reading it after a stop that had no collection window would
    # report whatever happens to be resident in the cgroup (including the collector
    # itself) as the run's peak, which is a fabricated measurement.
    if [ "${SAMPLE_COUNT:-0}" -gt 0 ]; then
        local cg_peak
        cg_peak="$(read_cgroup_file memory.peak)" || cg_peak=""
        case "$cg_peak" in
            '' | *[!0-9]*)
                warn "cgroup memory.peak unavailable; peak is a sampled lower bound"
                ;;
            *)
                if [ "$cg_peak" -gt 0 ]; then
                    MEM_CGROUP_PEAK="$cg_peak"
                    MEM_PEAK_SAMPLED="$cg_peak"
                    MEM_PEAK_SOURCE="cgroup.memory.peak"
                    MEM_PEAK_LOWER_BOUND="false"
                else
                    warn "cgroup memory.peak reported 0; peak is a sampled lower bound"
                fi
                ;;
        esac
    else
        warn "no samples collected; cgroup memory.peak is not attributed to this run"
    fi

    if [ -n "$log" ]; then
        if [ -r "$log" ]; then
            if load_log_facts "$log"; then
                : # phases came from log step markers
            else
                warn "no GitHub Actions step markers found in $log; phase durations unavailable"
            fi
        else
            warn "log file not readable: $log"
        fi
    else
        warn "no --log given: gradle cache and test metrics unavailable"
    fi

    if [ "$DUR_PHASES_SOURCE" = "unavailable" ]; then
        if load_marks "$out" "${start_epoch:-0}"; then
            DUR_PHASES_SOURCE="marks"
        else
            warn "phase durations unavailable (no log markers, no marks)"
        fi
    fi

    local now
    now="$(now_epoch_float)"
    if [ -n "$start_epoch" ]; then
        DUR_TOTAL="$(awk -v a="$start_epoch" -v b="$now" 'BEGIN { d = b - a; if (d < 0) d = 0; printf "%.1f", d }')"
    else
        warn "durationSeconds.total unavailable (no start time recorded)"
        DUR_TOTAL=""
    fi

    SAMPLE_INTERVAL="${interval:-null}"
    SAMPLE_TRUNCATED="$trunc_flag"
    ENV_CACHE_STATE="$(derive_cache_state)"

    fill_disk_and_oom

    if [ -z "${GC_FROM_CACHE:-}" ]; then
        warn "gradleCache.fromCacheTasks unavailable (no log parsed)"
    fi
    if [ -z "${GC_FAILED_READS:-}" ]; then
        warn "gradleCache.failedReads unavailable (no log parsed)"
    fi
    if [ -z "${TESTS_EXECUTED:-}" ]; then
        warn "tests.executed unavailable (no log parsed)"
    fi

    emit_resources || return 1
    info "resources.json: $out/resources.json"
    info "resources.csv:  $out/resources.csv"
    return 0
}

cmd_summarize() {
    local log="" out_path="" label="" out
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --log)
                log="${2:-}"
                shift 2
                ;;
            --out)
                out_path="${2:-}"
                shift 2
                ;;
            --label)
                label="${2:-}"
                shift 2
                ;;
            *) die "summarize: unexpected argument '$1'" ;;
        esac
    done
    [ -n "$log" ] || die "summarize: --log is required"
    [ -r "$log" ] || die "summarize: log file not readable: $log"

    reset_metrics
    OUT_DIR="$(resolve_out_dir "")"
    LABEL="${label:-unknown}"
    if [ -n "$out_path" ]; then
        OUT_DIR="$(dirname "$out_path")"
        mkdir -p "$OUT_DIR" 2>/dev/null || die "cannot create $(dirname "$out_path")"
    fi
    mkdir -p "$OUT_DIR" 2>/dev/null || warn "cannot create out-dir $OUT_DIR"

    if load_log_facts "$log"; then
        :
    else
        warn "no GitHub Actions step markers found in $log; phase durations unavailable"
    fi
    DUR_TOTAL=""
    SAMPLE_INTERVAL=""
    ENV_CACHE_STATE="$(derive_cache_state)"

    local ws="${GITHUB_WORKSPACE:-$(pwd)}"
    DISK_WORKSPACE="$(dir_bytes "$ws")" || DISK_WORKSPACE=""
    local gc_dir
    gc_dir="$(gradle_cache_dir)"
    DISK_GRADLE_CACHE="$(dir_bytes "$gc_dir")" || DISK_GRADLE_CACHE=""

    local ev
    ev="$(oom_events)" || ev=""
    if [ -n "$ev" ]; then
        OOM_EVENTS="$ev"
        OOM_EVIDENCE+=("cgroup memory.events oom+oom_kill=${ev}")
    else
        warn "oom.events unavailable (cgroup memory.events not readable)"
    fi

    local target="$OUT_DIR/resources.json"
    [ -n "$out_path" ] && target="$out_path"
    emit_resources || return 1
    if [ -n "$out_path" ] && [ "$target" != "$OUT_DIR/resources.json" ]; then
        cp -f "$OUT_DIR/resources.json" "$target" 2>/dev/null || warn "could not copy summary to $target"
    fi
    info "$target"
    return 0
}

cmd_selftest() {
    local out_dir="" out rc=0
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --out-dir)
                out_dir="${2:-}"
                shift 2
                ;;
            *) die "selftest: unexpected argument '$1'" ;;
        esac
    done
    out="$(resolve_out_dir "$out_dir")"
    mkdir -p "$out" || die "cannot create out-dir $out"

    # synthetic short-lived load: a sleeping process and a ~50 MB holder.
    # CI_RESOURCE_PROC_PATTERN makes the sampler match them (the default pattern only
    # matches java/docker/gradle, which are absent in a self test).
    export CI_RESOURCE_PROC_PATTERN='sleep|python3|python|dd'
    local load_pid="" py_pid=""
    sleep 8 &
    load_pid=$!
    if command -v python3 >/dev/null 2>&1; then
        python3 -c '
import time
buf = bytearray(50 * 1024 * 1024)
for i in range(0, len(buf), 4096):
    buf[i] = 1
time.sleep(8)
' &
        py_pid=$!
    else
        warn "python3 not available; only the sleep load generator is used"
    fi

    info "selftest: out-dir $out"
    info "selftest: load pids ${load_pid}${py_pid:+ $py_pid} (sleep + ~50MB holder, ~8s)"
    "$SELF_PATH" start --label selftest --out-dir "$out" --interval 1 >/dev/null || {
        warn "selftest: start failed"
        kill "$load_pid" 2>/dev/null
        [ -n "$py_pid" ] && kill "$py_pid" 2>/dev/null
        return 1
    }

    sleep 10

    kill "$load_pid" 2>/dev/null || true
    [ -n "$py_pid" ] && kill "$py_pid" 2>/dev/null || true
    wait "$load_pid" 2>/dev/null || true

    "$SELF_PATH" stop --label selftest --out-dir "$out" >/dev/null 2>&1 || true

    local ticks rss_peak sum_peak
    ticks="$(count_samples "$out/samples.jsonl")"
    rss_peak="$(jq -s 'map(.rssSumBytes // 0) | max // 0' "$out/samples.jsonl" 2>/dev/null)" || rss_peak="0"
    sum_peak="$(jq -r '.memoryBytes.sampledSumPeak // 0' "$out/resources.json" 2>/dev/null)" || sum_peak="0"
    [ -n "$rss_peak" ] || rss_peak="0"
    [ -n "$sum_peak" ] || sum_peak="0"

    info "selftest: observed ticks=${ticks} maxSimultaneousRssBytes=${rss_peak} sampledSumPeak=${sum_peak}"

    if [ "${ticks:-0}" -lt 2 ]; then
        warn "selftest FAILED: expected at least 2 sampler ticks, observed ${ticks}"
        rc=1
    fi
    if [ "${rss_peak:-0}" -le 0 ]; then
        warn "selftest FAILED: expected a non-zero simultaneous RSS sum, observed ${rss_peak}"
        rc=1
    fi
    if [ "$rc" -eq 0 ]; then
        info "selftest: OK (sampler observed the synthetic processes)"
    fi
    return "$rc"
}

usage() {
    cat <<'EOF'
collect-resources.sh - CI resource accounting collector (schemaVersion 1)

Usage:
  collect-resources.sh start --label <name> [--out-dir <dir>] [--interval <seconds>]
  collect-resources.sh mark <phase-name> [--out-dir <dir>]
  collect-resources.sh stop [--label <name>] [--out-dir <dir>] [--log <gradle-log-file>]
  collect-resources.sh summarize --log <gradle-log-file> [--out <json-path>] [--label <name>]
  collect-resources.sh selftest [--out-dir <dir>]
  collect-resources.sh --help

Out-dir default: ${CI_RESOURCE_OUT_DIR:-${RUNNER_TEMP:-${TMPDIR:-/tmp}}/ci-resources}

Environment:
  CI_RESOURCE_OUT_DIR        override the default out-dir
  CI_RESOURCE_SAMPLE_CAP     max lines kept in samples.jsonl (default 20000)
  CI_RESOURCE_PROC_PATTERN   regex of process names summed for RSS (default java|...)
  CI_RESOURCE_DOCKER_TIMEOUT seconds allowed per docker call (default 8)
  CI_RESOURCE_CACHE_STATE    force cold|warm instead of the log-derived heuristic

Outputs: state.json, samples.jsonl, marks.jsonl, resources.json, resources.csv,
and a summary appended to $GITHUB_STEP_SUMMARY when set.
EOF
}

main() {
    local cmd="${1:-}"
    [ "$#" -gt 0 ] && shift
    case "$cmd" in
        start)
            require_jq
            cmd_start "$@"
            ;;
        mark)
            require_jq
            cmd_mark "$@"
            ;;
        stop)
            require_jq
            cmd_stop "$@"
            ;;
        summarize)
            require_jq
            cmd_summarize "$@"
            ;;
        selftest)
            require_jq
            cmd_selftest "$@"
            ;;
        _sample)
            cmd_sample "$@"
            ;;
        -h | --help | help)
            usage
            exit 0
            ;;
        '')
            usage >&2
            exit 2
            ;;
        *)
            printf '%s: unknown command %s\n\n' "$SCRIPT_NAME" "$cmd" >&2
            usage >&2
            exit 2
            ;;
    esac
}

main "$@"
