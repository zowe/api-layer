#!/usr/bin/env bash
# Helper: run a gradle command in the PR4950 work clone with the right JDK and an English locale.
# The English locale matters: some tests assert JDK exception messages ("Connection refused",
# "Address already in use") which a non-English JVM renders differently.
set -o pipefail
export SDKMAN_DIR="$HOME/.sdkman"
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 17.0.20-sem >/dev/null 2>&1
export LANG=C
export LC_ALL=C
cd /home/balda/api-layer-pr4950
exec ./gradlew "$@" --console=plain
