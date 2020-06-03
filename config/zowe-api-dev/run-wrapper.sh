#!/bin/sh

export PATH=$PATH:$JAVA_HOME/bin

# Variables required on shell:
export ZOWE_PREFIX=ZOWEJ
export DISCOVERY_CODE=DS
export CATALOG_CODE=AC
export GATEWAY_CODE=GW
export ZOWE_EXPLORER_HOST=$systemHostname
export ZOWE_IP_ADDRESS=10.175.84.32
export DISCOVERY_PORT=$((basePort+1))
export CATALOG_PORT=$((basePort+2))
export DISCOVERABLECLIENT_PORT=$((basePort+3))
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

echo "*******************  ENVIRONMENT  *******************"
echo "Working directory "$dir
echo "JAVA_HOME "$JAVA_HOME
echo "PATH "$PATH
echo "basePort "$basePort
echo " "
env
echo "******************* /ENVIRONMENT  *******************"


echo "Inflating dependencies"
cd $dir/components/api-mediation/lib
jar -xvf libraries.zip
echo " "

echo "Running API Mediation Layer"
cd $dir
sh start.sh
