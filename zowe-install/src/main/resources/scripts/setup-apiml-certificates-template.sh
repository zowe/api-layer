#!/bin/sh
mkdir -p keystore/local_ca
mkdir -p keystore/localhost

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

EXT="SAN=dns:**HOSTNAME**,ip:**IPADDRESS**,dns:localhost.localdomain,dns:localhost,ip:127.0.0.1"

scripts/apiml_cm.sh --action setup --service-ext $EXT
