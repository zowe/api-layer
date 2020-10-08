export CLASSPATH=/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/lib/tools.jar:/usr/include/java_classes/IRRRacf.jar
export LIBPATH=/lib:/usr/lib:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/bin:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/bin/classic:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/bin/j9vm:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/lib/s390/classic:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/lib/s390/default:/sys/java64bt/v8r0m0/usr/lpp/java/J8.0_64/lib/s390/j9vm:
export JAVA_HOME=/usr/lpp/java/J8.0_64/
export PATH=$JAVA_HOME/bin:$PATH

export KEY_ALIAS=apiml
export KEY_PASSWORD=password
export KEYSTORE=/z/jb892003/apiml/keystore/apiml.keystore.p12
export KEYSTORE_TYPE=PKCS12
export KEYSTORE_PASSWORD=password
export TRUSTSTORE=/z/jb892003/apiml/keystore/apiml.truststore.p12
export TRUSTSTORE_TYPE=PKCS12
export TRUSTSTORE_PASSWORD=password

export HOSTNAME=usilca32.lvn.broadcom.net
export IP_ADDRESS=10.175.84.32

export GATEWAY_PORT=11920
export DISCOVERY_SERVICE_PORT=11921
export DISCOVERABLE_CLIENT_PORT=11923
export API_CATALOGUE_SERVICE_PORT=11922

export DISCOVERY_SERVICE_URL=https://${HOSTNAME}:${DISCOVERY_SERVICE_PORT}/eureka/
export DISCOVERY_STATIC_DEFINITION_DIR=/z/jb892003/apiml/api-defs

export ZOSMF_SERVICE_ID=zosmfca32

# Gateway service start up
java -Xms16m \
     -Xmx512m \
     -Dibm.serversocket.recover=true \
     -Dfile.encoding=UTF-8 \
     -Djava.io.tmpdir=/tmp \
     -Xquickstart \
     -Dspring.profiles.include=diag \
     -Dapiml.security.ssl.verifySslCertificatesOfServices=true \
     -Dapiml.service.scheme=https \
     -Dapiml.service.hostname=${HOSTNAME} \
     -Dapiml.service.port=${GATEWAY_PORT} \
     -Dapiml.service.discoveryServiceUrls=${DISCOVERY_SERVICE_URL} \
     -Dapiml.service.ipAddress=${IP_ADDRESS} \
     -Dserver.address=${IP_ADDRESS} \
     -Dapiml.service.preferIpAddress=false \
     -Dapiml.gateway.hostname=${HOSTNAME} \
     -Dapiml.gateway.timeoutMillis=30000 \
     -Dapiml.security.auth.zosmfServiceId=zosmfca32 \
     -DlogbackService=MAS1BGW1 \
     -Dserver.ssl.enabled=true \
     -Dserver.ssl.keyStore=${KEYSTORE} \
     -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
     -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
     -Dserver.ssl.keyAlias=${KEY_ALIAS} \
     -Dserver.ssl.keyPassword=${KEY_PASSWORD} \
     -Dserver.ssl.trustStore=${TRUSTSTORE} \
     -Dserver.ssl.trustStoreType=${TRUSTSTORE_TYPE} \
     -Dserver.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD} \
     -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
     -Dapiml.service.allowEncodedSlashes=true \
     -cp gateway-service.jar:/usr/include/java_classes/IRRRacf.jar \
    org.springframework.boot.loader.PropertiesLauncher > gateway.log &

# Discovery Service start up
java -Xms16m \
     -Xmx512m \
     -Dibm.serversocket.recover=true \
     -Dfile.encoding=UTF-8 \
     -Djava.io.tmpdir=/tmp \
     -Xquickstart \
     -Dspring.profiles.include=diag \
     -Dapiml.security.ssl.verifySslCertificatesOfServices=true \
     -Dspring.profiles.active=https \
     -Dapiml.discovery.allPeersUrls=${DISCOVERY_SERVICE_URL} \
     -Dapiml.service.hostname=${HOSTNAME} \
     -Dapiml.service.port=${DISCOVERY_SERVICE_PORT} \
     -Dapiml.service.ipAddress=${IP_ADDRESS} \
     -Dserver.address=${IP_ADDRESS} \
     -Dapiml.service.preferIpAddress=false \
     -Dapiml.discovery.staticApiDefinitionsDirectories=${DISCOVERY_STATIC_DEFINITION_DIR} \
     -DlogbackService=MAS1BDS1 \
     -Dserver.ssl.enabled=true \
     -Dserver.ssl.keyStore=${KEYSTORE} \
     -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
     -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
     -Dserver.ssl.keyAlias=${KEY_ALIAS} \
     -Dserver.ssl.keyPassword=${KEY_PASSWORD} \
     -Dserver.ssl.trustStore=${TRUSTSTORE} \
     -Dserver.ssl.trustStoreType=${TRUSTSTORE_TYPE} \
     -Dserver.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD} \
     -Djava.protocol.handler.pkgs=com.ibm.crypto.provider -jar discovery-service.jar > discoverService.log &

# Discoverable client start up
java -Xms16m \
     -Xmx512m \
     -Dibm.serversocket.recover=true \
     -Dfile.encoding=UTF-8 \
     -Djava.io.tmpdir=/tmp \
     -Xquickstart \
     -Dapiml.security.verifySslCertificatesOfServices=true \
     -Dapiml.service.id=discoverableclient \
     -Deureka.instance.metadata-map.discovery.service.title="Service Integration Enabler V2 Sample App (Spring boot 2.x)" \
     -Deureka.instance.metadata-map.discovery.service.description="SpringBoot REST API micro service" \
     -Deureka.client.enabled=true \
     -Dapiml.service.hostname=${HOSTNAME} \
     -Dapiml.service.port=${DISCOVERABLE_CLIENT_PORT} \
     -Dapiml.service.discoveryServiceUrls=${DISCOVERY_SERVICE_URL} \
     -Dapiml.service.ipAddress=${IP_ADDRESS} \
     -DlogbackService=MAS1BDC1 \
     -Dapiml.service.customMetadata.apiml.gatewayPort=${GATEWAY_PORT} \
     -Dserver.ssl.enabled=true \
     -Dserver.ssl.keyStore=${KEYSTORE} \
     -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
     -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
     -Dserver.ssl.keyAlias=${KEY_ALIAS} \
     -Dserver.ssl.keyPassword=${KEY_PASSWORD} \
     -Dserver.ssl.trustStore=${TRUSTSTORE} \
     -Dserver.ssl.trustStoreType=${TRUSTSTORE_TYPE} \
     -Dserver.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD} \
     -Djava.protocol.handler.pkgs=com.ibm.crypto.provider -jar discoverable-client.jar > discoverableClient.log &

# Api Catalogue start up
java -Xms16m \
     -Xmx512m \
     -Dibm.serversocket.recover=true \
     -Dfile.encoding=UTF-8 \
     -Djava.io.tmpdir=/tmp \
     -Xquickstart \
     -Dapiml.security.verifySslCertificatesOfServices=true \
     -Denvironment.hostname=${HOSTNAME} \
     -Denvironment.port=${API_CATALOGUE_SERVICE_PORT} \
     -Denvironment.discoveryLocations=${DISCOVERY_SERVICE_URL} \
     -Denvironment.ipAddress=${IP_ADDRESS} \
     -Denvironment.preferIpAddress=false \
     -Denvironment.gatewayHostname=${HOSTNAME} \
     -Denvironment.eurekaUserId= \
     -Denvironment.eurekaPassword= \
     -Dspring.profiles.include=diag \
     -Dapiml.security.zosmfServiceId=${ZOSMF_SERVICE_ID} \
     -DlogbackService=MAS1BAC1 \
     -Dserver.ssl.enabled=true \
     -Dserver.ssl.keyStore=${KEYSTORE} \
     -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
     -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
     -Dserver.ssl.keyAlias=${KEY_ALIAS} \
     -Dserver.ssl.keyPassword=${KEY_PASSWORD} \
     -Dserver.ssl.trustStore=${TRUSTSTORE} \
     -Dserver.ssl.trustStoreType=${TRUSTSTORE_TYPE} \
     -Dserver.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD} \
     -Denvironment.truststore=${TRUSTSTORE} \
     -Denvironment.truststoreType=${TRUSTSTORE_TYPE} \
     -Denvironment.truststorePassword=${TRUSTSTORE_PASSWORD} \
     -Djava.protocol.handler.pkgs=com.ibm.crypto.provider -jar api-catalog-services.jar > apiCatalog.log &
