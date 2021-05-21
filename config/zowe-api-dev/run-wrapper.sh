#!/bin/sh

export PATH=$PATH:$JAVA_HOME/bin

# Variables required on shell:
export ZOWE_PREFIX=ZOWEJ
export DISCOVERY_CODE=DS
export CATALOG_CODE=AC
export GATEWAY_CODE=GW
export CACHING_CODE=CS
export ZOWE_EXPLORER_HOST=$systemHostname
export ZOWE_IP_ADDRESS=0.0.0.0
export DISCOVERY_PORT=$((basePort+1))
export CATALOG_PORT=$((basePort+2))
export DISCOVERABLECLIENT_PORT=$((basePort+3))
export ZWE_CACHING_SERVICE_PORT=$((basePort+4))
export GATEWAY_PORT=$basePort
export STATIC_DEF_CONFIG_DIR=$dir/apidef
export VERIFY_CERTIFICATES=true
export KEY_ALIAS=apiml
export KEYSTORE=$dir/keystore/keystore.p12
export KEYSTORE_TYPE=PKCS12
export KEYSTORE_PASSWORD=password
export TRUSTSTORE=$dir/keystore/truststore.p12
export ROOT_DIR=$dir
export WORKSPACE_DIR=$dir
export ZOWE_MANIFEST=$dir/zowe-manifest.json
export ZWE_DISCOVERY_SERVICES_LIST=https://$ZOWE_EXPLORER_HOST:$DISCOVERY_PORT/eureka/
export APIML_PREFER_IP_ADDRESS=false
export APIML_ALLOW_ENCODED_SLASHES=true
export APIML_CORS_ENABLED=true
export APIML_GATEWAY_CATALOG_ID=apicatalog
export APIML_GATEWAY_TIMEOUT_MILLIS=3000
export APIML_SECURITY_AUTH_PROVIDER=saf
export ZWE_CACHING_EVICTION_STRATEGY=reject
export ZWE_CACHING_SERVICE_PERSISTENT=inMemory
echo "*******************  ENVIRONMENT  *******************"
echo "Working directory "$dir
echo "JAVA_HOME "$JAVA_HOME
echo "PATH "$PATH
echo "basePort "$basePort
echo " "
env
echo "******************* /ENVIRONMENT  *******************"

echo "Running API Mediation Layer"
cd $dir
sh start.sh
