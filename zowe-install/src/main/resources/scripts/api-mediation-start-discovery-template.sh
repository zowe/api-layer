# Variables to be replaced:
# - JAVA_SETUP -- sets JAVA_HOME by ZOWE_JAVA_HOME
# - IPADDRESS   -  The IP Address of the system running API Mediation
# - HOSTNAME   -  The hostname of the system running API Mediation (defaults to localhost)
# - DISCOVERY_PORT - The port the discovery service will use
# - CATALOG_PORT - The port the catalog service will use
# - GATEWAY_PORT - The port the gateway service will use
# - STATIC_DEF_CONFIG - The directory with statically defined APIs
# - VERIFY_CERTIFICATES - true/false - Validation of TLS/SSL certitificates for services

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

PARAMS="$@"

# The default log level is WARN
# Select one of the levels: ERROR | WARN | INFO | DEBUG | TRACE
LOG_LEVEL=WARN

function usage {
    echo "Set the log level for API Mediation Layer"
    echo "usage: api-mediation-start-discovery.sh -level <level>"
    echo ""
    echo "  <level> level to be setup:"
    echo "     - ERROR - setups APIML error level"
    echo "     - WARN - setups APIML warn level"
    echo "     - INFO - setups APIML info level"
    echo "     - DEBUG - setups APIML debug level"
    echo "     - TRACE - setups APIML trace level"
    echo ""
    echo "  Called with: ${PARAMS}"
}

while [ "$1" != "" ]; do
    case $1 in
        -l | --level )      shift
                                LOG_LEVEL=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     echo "Unexpected parameter: $1"
                                usage
                                exit 1
    esac
    shift
done

case $LOG_LEVEL in
    ERROR | WARN | INFO | DEBUG | TRACE )
        LEVEL=$LOG_LEVEL
        ;;
    *)
        usage
        exit 1
esac

DIR=`dirname $0`

java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=https \
    -Dspring.profiles.include=$LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.allPeersUrls=https://**HOSTNAME**:**DISCOVERY_PORT**/eureka/ \
    -Dapiml.service.hostname=**HOSTNAME** \
    -Dapiml.service.port=**DISCOVERY_PORT** \
    -Dapiml.service.ipAddress=**IPADDRESS** \
    -Dapiml.service.preferIpAddress=true \
    -Dapiml.discovery.staticApiDefinitionsDirectories=**STATIC_DEF_CONFIG** \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=**VERIFY_CERTIFICATES** \
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
    -jar $DIR/../discovery-service.jar &
