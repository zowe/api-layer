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

java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dapiml.service.hostname=**HOSTNAME** \
    -Dapiml.service.port=**GATEWAY_PORT** \
    -Dapiml.service.discoveryServiceUrls=http://eureka:password@**IPADDRESS**:**DISCOVERY_PORT**/eureka/ \
    -Dapiml.service.preferIpAddress=true \
    -Dapiml.service.ipAddress=**IPADDRESS** \
    -Dapiml.gateway.timeoutMillis=30000 \
    -Dserver.ssl.keystore=$DIR/../keystore/localhost/localhost.keystore.p12 \
    -Dserver.ssl.keystoreType=PKCS12 \
    -Dserver.ssl.keystorePassword=password \
    -Dserver.ssl.keyAlias=localhost \
    -Dserver.ssl.keyPassword=password \
    -Dserver.ssl.truststore=$DIR/../keystore/localhost/localhost.truststore.p12 \
    -Dserver.ssl.truststoreType=PKCS12 \
    -Dserver.ssl.truststorePassword=password \
    -jar $DIR/../gateway-service.jar &
