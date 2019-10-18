# Variables to be replaced:
# - JAVA_SETUP -- sets JAVA_HOME by ZOWE_JAVA_HOME
# - IPADDRESS   -  The IP Address of the system running API Mediation
# - HOSTNAME   -  The hostname of the system running API Mediation (defaults to localhost)
# - DISCOVERY_PORT - The port the discovery service will use
# - CATALOG_PORT - The port the catalog service will use
# - GATEWAY_PORT - The port the gateway service will use
# - VERIFY_CERTIFICATES - true/false - Validation of TLS/SSL certitificates for services

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

# API Mediation Layer Debug Mode
# To activate DEBUG mode, set LOG_LEVEL=DEBUG
LOG_LEVEL=

DIR=`dirname $0`

java -Xms16m -Xmx512m -Dibm.serversocket.recover=true -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp -Xquickstart -Denvironment.hostname=**HOSTNAME** -Denvironment.port=**CATALOG_PORT**  \
    -Denvironment.discoveryLocations=https://**HOSTNAME**:**DISCOVERY_PORT**/eureka/ -Denvironment.ipAddress=**IPADDRESS** \
    -Denvironment.preferIpAddress=true -Denvironment.gatewayHostname=**HOSTNAME** \
    -Denvironment.eurekaUserId=eureka \
    -Denvironment.eurekaPassword=password \
    -Dapiml.security.auth.zosmfServiceId=zosmf \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=**VERIFY_CERTIFICATES** \
    -Dspring.profiles.include=$LOG_LEVEL \
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
    -jar $DIR/../api-catalog-services.jar &
