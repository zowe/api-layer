#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019
################################################################################

CACHING_CODE=CS
  _BPX_JOBNAME=${ZOWE_PREFIX}${CACHING_CODE} java -Xms16m -Xmx512m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.port=${ZWE_CACHING_SERVICE_PORT} \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -jar ${ROOT_DIR}"/components/api-mediation/caching-service.jar" &
export CACHE_PID=$?
