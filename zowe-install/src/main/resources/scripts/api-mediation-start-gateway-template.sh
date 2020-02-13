# Variables to be replaced:
# - JAVA_SETUP - Sets JAVA_HOME by ZOWE_JAVA_HOME
# - IPADDRESS   -  The IP Address of the system running API Mediation
# - HOSTNAME   -  The hostname of the system running API Mediation (defaults to localhost)
# - DISCOVERY_PORT - The port the discovery service will use
# - CATALOG_PORT - The port the catalog service will use
# - GATEWAY_PORT - The port the gateway service will use
# - VERIFY_CERTIFICATES - true/false - Validation of TLS/SSL certitificates for services
# - ALLOW_SLASHES - true/false - Allows or prohibits the usage of encoded slashes in URL

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

# API Mediation Layer Debug Mode
# To activate `debug` mode, set LOG_LEVEL=debug (in lowercase)
LOG_LEVEL=

DIR=`dirname $0`

java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.hostname=**HOSTNAME** \
    -Dapiml.service.port=**GATEWAY_PORT** \
    -Dapiml.service.discoveryServiceUrls=https://**HOSTNAME**:**DISCOVERY_PORT**/eureka/ \
    -Dapiml.service.preferIpAddress=true \
    -Dapiml.service.ipAddress=**IPADDRESS** \
    -Dapiml.service.allowEncodedSlashes=**ALLOW_URL_SLASHES** \
    -Dapiml.gateway.timeoutMillis=30000 \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=**VERIFY_CERTIFICATES** \
    -Dapiml.security.auth.zosmfServiceId=zosmf \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=$DIR/../keystore/localhost/localhost.keystore.p12 \
    -Dserver.ssl.keyStoreType=PKCS12 \
    -Dserver.ssl.keyStorePassword=password \
    -Dserver.ssl.keyAlias=localhost \
    -Dserver.ssl.keyPassword=password \
    -Dserver.ssl.trustStore=$DIR/../keystore/localhost/localhost.truststore.p12 \
    -Dserver.ssl.trustStoreType=PKCS12 \
    -Dserver.ssl.trustStorePassword=password \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -jar $DIR/../gateway-service.jar &
