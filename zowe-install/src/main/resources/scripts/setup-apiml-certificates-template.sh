#!/bin/sh
mkdir -p keystore/local_ca
mkdir -p keystore/localhost

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH" >> $LOG_FILE
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..." >> $LOG_FILE
  export PATH=$PATH:$JAVA_HOME/bin
fi

EXT="SAN=dns:**HOSTNAME**,ip:**IPADDRESS**,dns:localhost.localdomain,dns:localhost,ip:127.0.0.1"

scripts/apiml_cm.sh --verbose --log $LOG_FILE --action setup --service-ext $EXT
RC=$?

echo "apiml_cm.sh returned: $RC" >> $LOG_FILE
if [ "$RC" -ne "0" ]; then
    echo "apiml_cm.sh has failed. See $LOG_FILE for more details"
    exit 1
fi
