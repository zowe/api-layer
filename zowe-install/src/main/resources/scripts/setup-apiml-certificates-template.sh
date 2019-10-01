#!/bin/sh

# Variables to be replaced:
# - HOSTNAME - The hostname of the system running API Mediation (defaults to localhost)
# - IPADDRESS - The IP Address of the system running API Mediation
# - VERIFY_CERTIFICATES - true/false - Should APIML verify certificates of services
# - ZOSMF_KEYRING - Name of the z/OSMF keyring
# - ZOSMF_USER - z/OSMF server user ID
# - EXTERNAL_CERTIFICATE - optional - Path to a PKCS12 keystore with a server certificate for APIML
# - EXTERNAL_CERTIFICATE_ALIAS - optional - Alias of the certificate in the keystore
# - EXTERNAL_CERTIFICATE_AUTHORITIES - optional - Public certificates of trusted CAs

echo "<setup-apiml-certificates.sh>" >> $LOG_FILE

mkdir -p keystore/local_ca
mkdir -p keystore/localhost

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH" >> $LOG_FILE
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..." >> $LOG_FILE
  export PATH=$PATH:$JAVA_HOME/bin
fi

SAN="SAN=dns:**HOSTNAME**,ip:**IPADDRESS**,dns:localhost.localdomain,dns:localhost,ip:127.0.0.1"

# If any external certificate fields are zero [blank], do not use the external setup method.
# If all external certificate fields are zero [blank], create everything from scratch.
# If all external fields are not zero [valid string], use external setup method.

if [[ -z "**EXTERNAL_CERTIFICATE**" ]] || [[ -z "**EXTERNAL_CERTIFICATE_ALIAS**" ]] || [[ -z "**EXTERNAL_CERTIFICATE_AUTHORITIES**" ]]; then
  if [[ -z "**EXTERNAL_CERTIFICATE**" ]] && [[ -z "**EXTERNAL_CERTIFICATE_ALIAS**" ]] && [[ -z "**EXTERNAL_CERTIFICATE_AUTHORITIES**" ]]; then
    scripts/apiml_cm.sh --verbose --log $LOG_FILE --action setup --service-ext ${SAN}
    RC=$?
    echo "apiml_cm.sh --action setup returned: $RC" >> $LOG_FILE
  else
    (>&2 echo "Zowe Install setup configuration is invalid; check your zowe-install.yaml file.")
    (>&2 echo "Some external apiml certificate fields are supplied...Fields must be filled out in full or left completely blank.")
    (>&2 echo "See $LOG_FILE for more details.")
    echo "</setup-apiml-certificates.sh>" >> $LOG_FILE
    exit 1
  fi
else
  EXT_CA_PARM=""
  for CA in **EXTERNAL_CERTIFICATE_AUTHORITIES**; do
      EXT_CA_PARM="${EXT_CA_PARM} --external-ca ${CA} "
  done

  scripts/apiml_cm.sh --verbose --log $LOG_FILE --action setup --service-ext ${SAN} \
    --external-certificate **EXTERNAL_CERTIFICATE** --external-certificate-alias **EXTERNAL_CERTIFICATE_ALIAS** ${EXT_CA_PARM}
  RC=$?

  echo "apiml_cm.sh --action setup returned: $RC" >> $LOG_FILE
fi

if [ "$RC" -ne "0" ]; then
    (>&2 echo "apiml_cm.sh --action setup has failed. See $LOG_FILE for more details")
    echo "</setup-apiml-certificates.sh>" >> $LOG_FILE
    exit 1
fi

if [[ "**VERIFY_CERTIFICATES**" == "true" ]]; then
  scripts/apiml_cm.sh --verbose --log $LOG_FILE --action trust-zosmf --zosmf-keyring **ZOSMF_KEYRING** --zosmf-userid **ZOSMF_USER**
  RC=$?

  echo "apiml_cm.sh --action trust-zosmf returned: $RC" >> $LOG_FILE
  if [ "$RC" -ne "0" ]; then
      (>&2 echo "apiml_cm.sh --action trust-zosmf has failed. See $LOG_FILE for more details")
      (>&2 echo "WARNING: z/OSMF is not trusted by the API Mediation Layer. Follow instructions in Zowe documentation about manual steps to trust z/OSMF")
      (>&2 echo "  Issue following commands as a user that has permissions to export public certificates from z/OSMF keyring:")
      (>&2 echo "    cd **ZOWE_ROOT_DIR**/components/api-mediation")
      (>&2 echo "    scripts/apiml_cm.sh --action trust-zosmf --zosmf-keyring **ZOSMF_KEYRING** --zosmf-userid **ZOSMF_USER**")
  fi
fi

echo "</setup-apiml-certificates.sh>" >> $LOG_FILE
