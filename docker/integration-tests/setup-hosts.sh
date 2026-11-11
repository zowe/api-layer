#!/bin/bash -e
# Adds/removes the loopback hostname aliases the integration-test compose
# stacks rely on, inside a marked block in /etc/hosts. The tests (run from the
# host - e.g. from an IDE) connect to services using the same hostnames the
# containers use to talk to each other; each compose file publishes the
# matching container ports to 127.0.0.1, so these aliases are what make
# "https://gateway-service:10010" resolve for a process running on the host.
#
# Safe to re-run: it only ever replaces its own marked block.
#
# Usage:
#   ./setup-hosts.sh add    apiml discoverable-client mock-services
#   ./setup-hosts.sh remove

HOSTS_FILE="/etc/hosts"
MARK_BEGIN="# >>> apiml integration-tests hosts (managed by docker/integration-tests/setup-hosts.sh) >>>"
MARK_END="# <<< apiml integration-tests hosts <<<"

action="$1"
shift || true

remove_block() {
    if grep -qF "${MARK_BEGIN}" "${HOSTS_FILE}" 2>/dev/null; then
        sudo sed -i.bak "/${MARK_BEGIN}/,/${MARK_END}/d" "${HOSTS_FILE}"
    fi
}

case "${action}" in
    add)
        if [ "$#" -eq 0 ]; then
            echo "Usage: $0 add <hostname> [hostname...]" >&2
            exit 1
        fi
        remove_block
        {
            echo "${MARK_BEGIN}"
            for host in "$@"; do
                echo "127.0.0.1 ${host}"
            done
            echo "${MARK_END}"
        } | sudo tee -a "${HOSTS_FILE}" > /dev/null
        echo "Added: $*"
        ;;
    remove)
        remove_block
        echo "Removed the managed hosts block (if it was present)"
        ;;
    *)
        echo "Usage: $0 add <hostname> [hostname...] | remove" >&2
        exit 1
        ;;
esac
