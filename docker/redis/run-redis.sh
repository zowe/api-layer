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

SCRIPT_PWD=$(cd "$(dirname "$0")" && pwd)
WORKSPACE="${SCRIPT_PWD}/redis-containers"
KEYSTORE_DIR="${WORKSPACE}/keystore"
CONFIG_DIR="config"
REDIS_COMPOSE_FILE="redis.yml"
APIML_ENV_TEMPLATE="apiml.env.template"

SENTINEL="false"
TLS="false"
LINUX="false"
while getopts slta: arg
do
    case "${arg}" in
        s) SENTINEL="true";;
        l) LINUX="true";;
        t) TLS="true";;
        a) APIML_VERSION="${OPTARG}"
    esac
done

if [ -z $(command -v docker compose) ]; then
  echo "[${SCRIPT_NAME}][error] docker-compose is required."
  exit 1
fi

if [ -z "${APIML_VERSION}" ]; then
    APIML_VERSION="latest"
fi

dockerComposeFile="compose"
if [ "${SENTINEL}" == "true" ]; then
    dockerComposeFile="${dockerComposeFile}/sentinel"
else
    dockerComposeFile="${dockerComposeFile}/replica"
fi

if [ "${LINUX}" == "true" ]; then
    REDIS_MASTER_HOST="localhost"
    dockerComposeFile="${dockerComposeFile}-linux"
else
    REDIS_MASTER_HOST="redis-master"
fi
dockerComposeFile="${dockerComposeFile}.yml.template"

MASTER_CONFIG="${CONFIG_DIR}/master.conf"
REPLICA_TEMPLATE="${CONFIG_DIR}/replica.conf.template"
SENTINEL_TEMPLATE="${CONFIG_DIR}/sentinel.conf.template"
SENTINEL_ANNOUNCE_HOSTNAMES="
sentinel announce-hostnames yes
"
TLS_CONFIG="
# TLS Configuration
tls-cert-file /usr/local/etc/keystore/localhost/localhost.keystore.cer
tls-key-file /usr/local/etc/keystore/localhost/localhost.keystore.key
tls-ca-cert-file /usr/local/etc/keystore/local_ca/zowe-dev-ca.cer
tls-auth-clients no
tls-replication yes
# overwrites any port directive above
port 0
tls-port {TLS_PORT}"

rm -rf "${WORKSPACE}"
mkdir -p "${WORKSPACE}"
mkdir -p "${WORKSPACE}/${CONFIG_DIR}"
mkdir -p "${KEYSTORE_DIR}"
mkdir -p "${KEYSTORE_DIR}/docker"

keytool -genkeypair -v \
  -alias localca \
  -keyalg RSA -keysize 2048 \
  -keystore "${KEYSTORE_DIR}/docker/localca.keystore.p12" \
  -dname "CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12 \
  -validity 3650 \
  -ext KeyUsage=keyCertSign -ext BasicConstraints:critical=ca:true
keytool -export -v \
  -alias localca \
  -file "${KEYSTORE_DIR}/docker/localca.cer" \
  -keystore "${KEYSTORE_DIR}/docker/localca.keystore.p12" \
  -rfc \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12
keytool -genkeypair -v \
  -alias localhost \
  -keyalg RSA -keysize 2048 \
  -keystore "${KEYSTORE_DIR}/docker/all-services.keystore.p12" \
  -keypass password \
  -storepass password \
  -storetype PKCS12 \
  -dname "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -validity 3650
keytool -certreq -v \
  -alias localhost \
  -keystore "${KEYSTORE_DIR}/docker/all-services.keystore.p12" \
  -storepass password \
  -file "${KEYSTORE_DIR}/docker/all-services.keystore.csr" \
  -keyalg RSA -storetype PKCS12 \
  -dname "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -validity 3650
keytool -gencert -v \
  -infile "${KEYSTORE_DIR}/docker/all-services.keystore.csr" \
  -outfile "${KEYSTORE_DIR}/docker/all-services.keystore_signed.cer" \
  -keystore "${KEYSTORE_DIR}/docker/localca.keystore.p12" \
  -alias localca \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12 \
  -ext "SAN=dns:localhost,ip:127.0.0.1,dns:gateway-service,dns:discovery-service,dns:caching-service,dns:mock-services,dns:redis-master,dns:redis-replica,dns:redis-sentinel-1,dns:redis-sentinel-2,dns:redis-sentinel-3" \
  -ext "KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment" \
  -ext "ExtendedKeyUsage=clientAuth,serverAuth" \
  -rfc \
  -validity 3650
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${KEYSTORE_DIR}/docker/localca.cer" \
  -alias localca \
  -keystore "${KEYSTORE_DIR}/docker/all-services.keystore.p12" \
  -storepass password \
  -storetype PKCS12
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${KEYSTORE_DIR}/docker/all-services.keystore_signed.cer" \
  -alias localhost \
  -keystore "${KEYSTORE_DIR}/docker/all-services.keystore.p12" \
  -storepass password \
  -storetype PKCS12
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${KEYSTORE_DIR}/docker/localca.cer" \
  -alias localca \
  -keystore "${KEYSTORE_DIR}/docker/all-services.truststore.p12" \
  -storepass password \
  -storetype PKCS12
openssl pkcs12 -in "${KEYSTORE_DIR}/docker/all-services.keystore.p12" -nocerts -out "${KEYSTORE_DIR}/docker/all-services.keystore.key" -passin pass:password -passout pass:password

cp "${dockerComposeFile}" "${WORKSPACE}/${REDIS_COMPOSE_FILE}"
cp "${MASTER_CONFIG}" "${WORKSPACE}/${CONFIG_DIR}"
cp -R "${SCRIPT_PWD}/../../config/docker/api-defs" "${WORKSPACE}"
cp "${SCRIPT_PWD}/compose/mock-services.yml" "${WORKSPACE}/api-defs/mock-services-localhost.yml"

sed -e "s|{APIML_VERSION}|${APIML_VERSION}|g" \
    "${dockerComposeFile}" > "${WORKSPACE}/${REDIS_COMPOSE_FILE}"

sed -e "s|{SENTINEL}|${SENTINEL}|g" \
    -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
    -e "s|{TLS}|${TLS}|g" \
    "compose/${APIML_ENV_TEMPLATE}" > "${WORKSPACE}/apiml.env"

sed -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
    "${REPLICA_TEMPLATE}" > "${WORKSPACE}/${CONFIG_DIR}/replica.conf"

sed -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
    -e "s|{SENTINEL_PORT}|26379|g" \
    "${SENTINEL_TEMPLATE}" > "${WORKSPACE}/${CONFIG_DIR}/sentinel-1.conf"

sed -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
    -e "s|{SENTINEL_PORT}|26380|g" \
    "${SENTINEL_TEMPLATE}" > "${WORKSPACE}/${CONFIG_DIR}/sentinel-2.conf"

sed -e "s|{REDIS_MASTER_HOST}|${REDIS_MASTER_HOST}|g" \
    -e "s|{SENTINEL_PORT}|26381|g" \
    "${SENTINEL_TEMPLATE}" > "${WORKSPACE}/${CONFIG_DIR}/sentinel-3.conf"

if [ "${LINUX}" == "false" ]; then
    echo "${SENTINEL_ANNOUNCE_HOSTNAMES}" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-1.conf"
    echo "${SENTINEL_ANNOUNCE_HOSTNAMES}" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-2.conf"
    echo "${SENTINEL_ANNOUNCE_HOSTNAMES}" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-3.conf"
fi
if [ "${TLS}" == "true" ]; then
    echo "${TLS_CONFIG}" | sed "s/{TLS_PORT}/6379/" >> "${WORKSPACE}/${CONFIG_DIR}/master.conf"
    echo "${TLS_CONFIG}" | sed "s/{TLS_PORT}/6380/" >> "${WORKSPACE}/${CONFIG_DIR}/replica.conf"
    echo "${TLS_CONFIG}" | sed "s/{TLS_PORT}/26379/" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-1.conf"
    echo "${TLS_CONFIG}" | sed "s/{TLS_PORT}/26380/" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-2.conf"
    echo "${TLS_CONFIG}" | sed "s/{TLS_PORT}/26381/" >> "${WORKSPACE}/${CONFIG_DIR}/sentinel-3.conf"
fi

docker compose -f "${WORKSPACE}/${REDIS_COMPOSE_FILE}" up -d
exit 0
