#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright Contributors to the Zowe Project.
################################################################################

################################################################################
# Retire a persistent Infinispan cache directory of the Caching Service.
#
# Removing a cache definition from the product does not remove its data. Each persistent cache lives in
# <workspace>/caching-service/<haInstanceId>/<cacheName>/, and with the default 256 segments even an empty
# one costs about a megabyte across 258 index files. Nothing in the product has ever cleaned one up, so an
# installation that has been upgraded a few times carries directories for caches that no longer exist.
#
# This script does not delete anything by default: it renames the directory to <cacheName>.retired-<stamp>
# and reports how much space that frees. Deleting is a second, explicit choice, because a revocation store is
# not something to remove out from under an operator by surprise.
#
# WHEN TO RUN IT for 'zoweInvalidatedTokenCache', the pre-cutover personal access token revocation store:
#
#   * Any time after upgrading to the release that introduced the per-item store. Reclaims the disk at once
#     and puts every personal access token request on the fast path immediately. THE COST, IN AS MANY WORDS:
#     every revocation made BEFORE the upgrade stops being enforced, so a pre-upgrade token that was revoked
#     works again until it expires on its own. Re-revoke anything that still matters.
#
#   * More than 90 days after the upgrade, or after the release that removes the pre-cutover read path.
#     No consequence at all - nothing reads the directory any more, and every token it could have applied to
#     has expired regardless.
#
# Usage:
#   retire-cache.sh --list                       list the cache directories that can be retired, with sizes
#   retire-cache.sh <cacheName>                  rename the directory aside (reversible)
#   retire-cache.sh <cacheName> --delete         remove the directory for good
#
# Options:
#   --workspace <dir>   override the workspace directory (default: $ZWE_zowe_workspaceDirectory)
#   --instance <id>     override the HA instance id     (default: $ZWE_haInstance_id, else 'localhost')
#   --force             proceed although Infinispan's lock file is present - only for an instance that is
#                       known to be stopped after an unclean shutdown
#
# Works for both the Caching Service and the API Mediation Layer modulith, which keeps its store in the
# same place. The instance must be stopped first.
################################################################################

set -e

# zoweCache holds the personal access token hashing salt: losing it un-enforces every revocation there is,
# on every node, permanently. It is never a candidate.
PROTECTED_NAMES="zoweCache"

# Shared cache-manager state, structurally separate from any one cache. LazyCacheManager has its own
# recovery path for these; moving them by hand is a different operation with different consequences.
GLOBAL_STATE_NAMES="___global.state ___global.lck"

workspace="${ZWE_zowe_workspaceDirectory:-}"
instance="${ZWE_haInstance_id:-localhost}"
cache_name=""
do_list="false"
do_delete="false"
do_force="false"

while [ $# -gt 0 ]; do
    case "$1" in
        --list)      do_list="true" ;;
        --delete)    do_delete="true" ;;
        --force)     do_force="true" ;;
        --workspace) shift; workspace="$1" ;;
        --instance)  shift; instance="$1" ;;
        -h|--help)   sed -n '13,49p' "$0"; exit 0 ;;
        -*)          echo "Unknown option: $1" >&2; exit 2 ;;
        *)
            if [ -n "${cache_name}" ]; then
                echo "Only one cache name can be given at a time" >&2
                exit 2
            fi
            cache_name="$1"
            ;;
    esac
    shift
done

if [ -z "${workspace}" ]; then
    root="caching-service/${instance}"
else
    root="${workspace}/caching-service/${instance}"
fi

if [ ! -d "${root}" ]; then
    echo "No Caching Service data directory at ${root}" >&2
    echo "Set ZWE_zowe_workspaceDirectory, or pass --workspace <dir>." >&2
    exit 1
fi

size_of() {
    du -sk "$1" 2>/dev/null | awk '{printf "%.1f MB", $1/1024}'
}

is_protected() {
    for protected in ${PROTECTED_NAMES}; do
        if [ "$1" = "${protected}" ]; then return 0; fi
    done
    for global in ${GLOBAL_STATE_NAMES}; do
        if [ "$1" = "${global}" ]; then return 0; fi
    done
    return 1
}

if [ "${do_list}" = "true" ] || [ -z "${cache_name}" ]; then
    echo "Cache directories under ${root}:"
    echo ""
    for dir in "${root}"/*; do
        [ -d "${dir}" ] || continue
        name=$(basename "${dir}")
        case "${name}" in
            *.retired-*) note="  (already retired)" ;;
            *)
                if is_protected "${name}"; then
                    note="  (protected - never retire this)"
                else
                    note=""
                fi
                ;;
        esac
        printf '  %-40s %10s%s\n' "${name}" "$(size_of "${dir}")" "${note}"
    done
    echo ""
    if [ -z "${cache_name}" ] && [ "${do_list}" != "true" ]; then
        echo "Pass a cache name to retire one of them." >&2
        exit 2
    fi
    exit 0
fi

if is_protected "${cache_name}"; then
    echo "Refusing to touch '${cache_name}': it is either shared cache-manager state or holds data that" >&2
    echo "cannot be regenerated (the personal access token hashing salt)." >&2
    exit 1
fi

# The directory must not be live when it is moved. Infinispan keeps open file handles into it, and renaming
# underneath a running instance corrupts the store rather than retiring it.
#
# The authoritative check is Infinispan's own lock file: the cache manager holds it for as long as it runs and
# deletes it on a clean stop, so it covers exactly this directory, whichever process owns it. It is also left
# behind by an unclean shutdown, which is what --force is for.
if [ -e "${root}/___global.lck" ] && [ "${do_force}" != "true" ]; then
    echo "Infinispan's lock file is present: ${root}/___global.lck" >&2
    echo "Either the Caching Service (or the API Mediation Layer, in a modulith deployment) is still running" >&2
    echo "against this directory, or it was not shut down cleanly. Stop it before retiring a cache directory." >&2
    echo "If it is certainly stopped, start and stop it once to clear the lock, or pass --force." >&2
    exit 1
fi

# A second, best-effort check by process name, for a store whose lock file is missing for any other reason.
# The modulith runs apiml-lite.jar rather than caching-service.jar, and uses the same directory.
service_pid=$(ps -ef 2>/dev/null | grep -E '[c]aching-service.*\.jar|[a]piml-lite.*\.jar' | awk '{print $2}' | head -1 || true)
if [ -n "${service_pid}" ] && [ "${do_force}" != "true" ]; then
    echo "The Caching Service or the API Mediation Layer still looks to be running (pid ${service_pid})." >&2
    echo "Stop it before retiring a cache directory - Infinispan holds open handles into it." >&2
    exit 1
fi

target="${root}/${cache_name}"
if [ ! -d "${target}" ]; then
    echo "No such cache directory: ${target}" >&2
    echo "Run '$0 --list' to see what is there." >&2
    exit 1
fi

reclaimed=$(size_of "${target}")

if [ "${do_delete}" = "true" ]; then
    rm -rf "${target}"
    echo "Deleted ${target} - reclaimed ${reclaimed}"
else
    stamp=$(date -u '+%Y%m%dT%H%M%SZ')
    retired="${target}.retired-${stamp}"
    mv "${target}" "${retired}"
    echo "Moved ${target} aside to ${retired} (${reclaimed})"
    echo "Nothing reads it any more. Delete it when you are satisfied:"
    echo "  rm -rf ${retired}"
fi
