#!/bin/sh

export PATH=$PATH:$JAVA_HOME/bin

# Variables required on shell:
# This list should be exhaustive, with variables that are not needed commented
# sorted alphabetically for easier maintenance where possible
export APIML_ALLOW_ENCODED_SLASHES=true
export APIML_CORS_ENABLED=true
export APIML_DEBUG_MODE_ENABLED=true
export APIML_DIAG_MODE_ENABLED=false
export APIML_GATEWAY_CATALOG_ID=apicatalog
#export APIML_GATEWAY_EXTERNAL_MAPPER
#export APIML_GATEWAY_INTERNAL_ENABLED:-false
#export APIML_GATEWAY_INTERNAL_PORT:-10017
#export APIML_GATEWAY_INTERNAL_SSL_KEY_ALIAS:-localhost-multi
#export APIML_GATEWAY_INTERNAL_SSL_KEYSTORE:-keystore/localhost/localhost-multi.keystore.p12
#export APIML_GATEWAY_MAPPER_USER:-ZWESVUSR
export APIML_GATEWAY_TIMEOUT_MILLIS=30000
#export APIML_MAX_CONNECTIONS_PER_ROUTE:-10
#export APIML_MAX_TOTAL_CONNECTIONS:-100
export APIML_PREFER_IP_ADDRESS=false
export APIML_SECURITY_AUTH_PROVIDER=zosmf
#export APIML_SECURITY_AUTHORIZATION_ENDPOINT_ENABLED:-false
#export APIML_SECURITY_AUTHORIZATION_ENDPOINT_URL
#export APIML_SECURITY_AUTHORIZATION_PROVIDER:-
export APIML_SECURITY_X509_ENABLED=false
export APIML_SECURITY_ZOSMF_APPLID=IZUDFLT
#export APIML_SECURITY_ZOSMF_JWT_AUTOCONFIGURATION_MODE:-auto
#export APIML_SPRING_PROFILES:-
#export APIML_SPRING_PROFILES:-https THIS NEEDS CHANGING ${APIML_SPRING_PROFILES:-https}
#export APIML_SSL_ENABLED:-true
export APIML_ZOSMF_ID=zosmf
export CACHING_CODE=CS
export CATALOG_CODE=AC
export CATALOG_PORT=$((basePort+2))
#export CMMN_LB=
export COMMON_LIB=components/api-mediation/lib/api-layer-lite-lib-all.jar,/usr/include/java_classes/IRRRacf.jar
export DISCOVERY_CODE=DS
export DISCOVERY_PORT=$((basePort+1))
export DISCOVERABLECLIENT_PORT=$((basePort+3))
export GATEWAY_CODE=GW
export GATEWAY_LOADER_PATH=components/api-mediation/lib/api-layer-lite-lib-all.jar,/usr/include/java_classes/IRRRacf.jar
export GATEWAY_PORT=$basePort
#export JAR_FILE #MODDED
export KEY_ALIAS=apiml
export KEYSTORE_PASSWORD=password
export KEYSTORE_TYPE=PKCS12
export KEYSTORE=$dir/keystore/keystore.p12
#export LAUNCH_COMPONENT
#export LIBRARY_PATH
#export NONSTRICT_VERIFY_CERTIFICATES:-false
#export QUICK_START
#export RESOURCE_CLASS:-ZOWE
#export RESOURCE_NAME_PREFIX:-APIML.
export ROOT_DIR=$dir
#export STATIC_DEF_CONFIG_DIR
export TRUSTSTORE=$dir/keystore/truststore.p12
export VERIFY_CERTIFICATES=true
#export VSAM_FILE_NAME
export WORKSPACE_DIR=$dir
export ZOWE_EXPLORER_HOST=$systemHostname
export ZOWE_IP_ADDRESS=0.0.0.0
export ZOWE_MANIFEST=$dir/zowe-manifest.json
export ZOWE_PREFIX=ZOWEJ
export ZWE_CACHING_EVICTION_STRATEGY=reject
export ZWE_CACHING_SERVICE_PERSISTENT=infinispan
export ZWE_CACHING_SERVICE_PORT=$((basePort+4))
#export ZWE_CACHING_SERVICE_VSAM_DATASET
#export ZWE_CACHING_STORAGE_SIZE
export ZWE_DISCOVERY_SERVICES_LIST=https://$ZOWE_EXPLORER_HOST:$DISCOVERY_PORT/eureka/
export ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES=$dir/apidef
export APIML_STATIC_DEF=$ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES
#export ZWE_configs_apiml_discovery_serviceIdPrefixReplacer=
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
