#!/bin/bash -e

###################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2020
###################################################################

# Run containerized Redis setup

log() {
    echo ">>>>> $1"
}

createFile() {
    log "Creating $2"
    sed -e "s|{APIML_VERSION}|${APIML_VERSION}|g" \
        -e "s|{GATEWAY_VERSION}|${GATEWAY_VERSION}|g" \
        -e "s|{MOCK_ZOSMF_HOST}|${MOCK_ZOSMF_HOST}|g" \
        -e "s|{SENTINEL}|${SENTINEL}|g" \
        -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
        -e "s|{TLS}|${TLS}|g" \
        -e "s|{TLS_SETTING}|${TLS_SETTING}|g" \
        -e "s|{TLS_PORT}|$3|g" \
        -e "s|{LINUX_SETTING}|${LINUX_SETTING}|g" \
        -e "s|{NOT_LINUX_SETTING}|${NOT_LINUX_SETTING}|g" \
        -e "s|{SENTINEL_SETTING}|${SENTINEL_SETTING}|g" \
        -e "s|{SENTINEL_PORT}|$3|g" \
        $1 > $2
}

genKeyPairCert() {
    keyAlias=$1
    keytool -genkeypair -v \
      -alias "${keyAlias}" \
      -keyalg RSA -keysize 2048 \
      -keystore "${KEYSTORE}" \
      -keypass password \
      -storepass password \
      -storetype PKCS12 \
      -dname "CN=Zowe Service ${keyAlias}, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
      -validity 3650
    keytool -certreq -v \
      -alias "${keyAlias}" \
      -keystore "${KEYSTORE}" \
      -storepass password \
      -file "${KEYSTORE_DIR}/all-services-${keyAlias}.keystore.csr" \
      -keyalg RSA -storetype PKCS12 \
      -dname "CN=Zowe Service ${keyAlias}, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
      -validity 3650
    keytool -gencert -v \
      -infile "${KEYSTORE_DIR}/all-services-${keyAlias}.keystore.csr" \
      -outfile "${KEYSTORE_DIR}/all-services-${keyAlias}.keystore_signed.cer" \
      -keystore "${CA_KEYSTORE}" \
      -alias "${CA_ALIAS}" \
      -keypass local_ca_password \
      -storepass local_ca_password \
      -storetype PKCS12 \
      -ext "SAN=dns:localhost,ip:127.0.0.1,ip:::1,dns:gateway-service,dns:discovery-service,dns:caching-service,dns:mock-services,dns:redis-master,dns:redis-replica,dns:redis-sentinel-1,dns:redis-sentinel-2,dns:redis-sentinel-3" \
      -ext "KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment" \
      -ext "ExtendedKeyUsage=clientAuth,serverAuth" \
      -rfc \
      -validity 3650
    keytool -importcert -v \
      -trustcacerts -noprompt \
      -file "${KEYSTORE_DIR}/all-services-${keyAlias}.keystore_signed.cer" \
      -alias "${keyAlias}" \
      -keystore "${KEYSTORE}" \
      -storepass password \
      -storetype PKCS12
}

SCRIPT_PWD=$(cd "$(dirname "$0")" && pwd)
WORKSPACE="${SCRIPT_PWD}/redis-containers"
KEYSTORE_DIR="${WORKSPACE}/keystore"
COMPOSE_DIR="${SCRIPT_PWD}/compose"
CONFIG_DIR="${SCRIPT_PWD}/config"
REDIS_COMPOSE_FILE="redis.yml"
GATEWAY_VERSION="7749575670-947"

LINUX_SETTING="#"
NOT_LINUX_SETTING=""
SENTINEL_SETTING="#"
TLS_SETTING="#"

SENTINEL="false"
TLS="false"
LINUX="false"
WHAT_IF="false"
while getopts slta:W arg
do
    case "${arg}" in
        s) SENTINEL="true";;
        l) LINUX="true";;
        t) TLS="true";;
        a) APIML_VERSION="${OPTARG}";;
        W) WHAT_IF="true"
    esac
done

if [ -z $(command -v docker compose) ]; then
  echo "[${SCRIPT_NAME}][error] docker compose is required."
  exit 1
fi

if [ -z "${APIML_VERSION}" ]; then
    APIML_VERSION="latest"
fi

if [ "${TLS}" == "true" ]; then
    TLS_SETTING=""
fi

if [ "${SENTINEL}" == "true" ]; then
    SENTINEL_SETTING=""
fi

REDIS_MASTER_HOST="redis-master"
MOCK_ZOSMF_HOST="mock-services"
if [ "${LINUX}" == "true" ]; then
    REDIS_MASTER_HOST="localhost"
    MOCK_ZOSMF_HOST="localhost"
    LINUX_SETTING=""
    NOT_LINUX_SETTING="#"
fi

log "Wiping and re-creating $WORKSPACE"
rm -rf "${WORKSPACE}"
mkdir -p "${WORKSPACE}/config"
mkdir -p "${WORKSPACE}/api-defs"
mkdir -p "${KEYSTORE_DIR}"

KEYSTORE="${KEYSTORE_DIR}/all-services.keystore.p12"
CA_KEYSTORE="${KEYSTORE_DIR}/localca.keystore.p12"
CA_ALIAS="apiml external certificate authority"

log "Creating CA, keystore, and truststore"
keytool -genkeypair -v \
  -alias "${CA_ALIAS}" \
  -keyalg RSA -keysize 2048 \
  -keystore "${CA_KEYSTORE}" \
  -dname "CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12 \
  -validity 3650 \
  -ext KeyUsage=keyCertSign -ext BasicConstraints:critical=ca:true
keytool -export -v \
  -alias "${CA_ALIAS}" \
  -file "${KEYSTORE_DIR}/localca.cer" \
  -keystore "${CA_KEYSTORE}" \
  -rfc \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${KEYSTORE_DIR}/localca.cer" \
  -alias "${CA_ALIAS}" \
  -keystore "${KEYSTORE}" \
  -storepass password \
  -storetype PKCS12
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${KEYSTORE_DIR}/localca.cer" \
  -alias "${CA_ALIAS}" \
  -keystore "${KEYSTORE_DIR}/all-services.truststore.p12" \
  -storepass password \
  -storetype PKCS12
genKeyPairCert apimtst
genKeyPairCert user
genKeyPairCert unknownuser
log "Extracting private key"
openssl pkcs12 -in "${KEYSTORE}" -legacy -nocerts -out "${KEYSTORE_DIR}/all-services.keystore.key" -passin pass:password -passout pass:password

log "Setting permissions for keystore files"
chmod -R 775 "${KEYSTORE_DIR}"

DOCKER_COMPOSE_TEMPLATE="${COMPOSE_DIR}/redis.yml.template"
APIML_ENV_TEMPLATE="${COMPOSE_DIR}/apiml.env.template"
MOCK_SERVICES_TEMPLATE="${COMPOSE_DIR}/mock-services.yml.template"
MASTER_TEMPLATE="${CONFIG_DIR}/master.conf.template"
REPLICA_TEMPLATE="${CONFIG_DIR}/replica.conf.template"
SENTINEL_TEMPLATE="${CONFIG_DIR}/sentinel.conf.template"

createFile "${DOCKER_COMPOSE_TEMPLATE}" "${WORKSPACE}/${REDIS_COMPOSE_FILE}"
createFile "${MOCK_SERVICES_TEMPLATE}" "${WORKSPACE}/api-defs/mock-services.yml"
createFile "${APIML_ENV_TEMPLATE}" "${WORKSPACE}/apiml.env"

createFile "${MASTER_TEMPLATE}" "${WORKSPACE}/config/master.conf" "6379"
createFile "${REPLICA_TEMPLATE}" "${WORKSPACE}/config/replica.conf" "6380"

createFile "${SENTINEL_TEMPLATE}" "${WORKSPACE}/config/sentinel-1.conf" "26739"
createFile "${SENTINEL_TEMPLATE}" "${WORKSPACE}/config/sentinel-2.conf" "26380"
createFile "${SENTINEL_TEMPLATE}" "${WORKSPACE}/config/sentinel-3.conf" "26381"

if [ "${WHAT_IF}" != "true" ]; then
    log "Running containers"
    docker compose -f "${WORKSPACE}/${REDIS_COMPOSE_FILE}" up -d
else
    log "-W argument was used so containers were not started"
fi
exit 0
