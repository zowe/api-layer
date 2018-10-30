# Variables to be replace:
# - JAVA_SETUP -- sets JAVA_HOME by ZOWE_JAVA_HOME
# - IPADDRESS   -  The IP Address of the system running API Mediation
# - HOSTNAME   -  The hostname of the system running API Mediation (defaults to localhost)
# - DISCOVERY_PORT - The port the discovery service will use
# - CATALOG_PORT - The port the catalog service will use
# - GATEWAY_PORT - The port the gateway service will use

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

DIR=`dirname $0`

java -Xms16m -Xmx512m -Dibm.serversocket.recover=true -Dfile.encoding=UTF-8 -Djava.io.tmpdir=/tmp -Xquickstart \
	-Denvironment.gatewayHostname=**HOSTNAME** -Denvironment.hostname=**HOSTNAME** -Denvironment.port=**GATEWAY_PORT** \
	-Denvironment.discoveryLocations=http://eureka:password@**IPADDRESS**:**DISCOVERY_PORT**/eureka/ -Denvironment.dsIpAddress=0.0.0.0 \
	-Denvironment.preferIpAddress=true -Denvironment.ipAddress=**IPADDRESS** -Denvironment.gatewayTimeoutInMillis=30000 \
	-Denvironment.sslEnabled=true -Denvironment.scheme=https -Denvironment.keystore=$DIR/../keystore/api_gateway.ks \
	-Denvironment.keystoreType=JKS -Denvironment.keystorePassword=zoe_password -Denvironment.keyAlias=api_gateway -Denvironment.keyPassword=zoe_password \
	-Denvironment.truststore=$DIR/../keystore/api_gateway.ts -Denvironment.truststoreType=JKS \
	-Denvironment.truststorePassword=zoe_password \
	-jar $DIR/../gateway-service.jar &
