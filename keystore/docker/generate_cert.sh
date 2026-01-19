#!/bin/sh

set -e

if [ $# != 2 ]; then
    echo "Arguments were not provided or they are invalid:"
    echo " 1. argument: <all-services.keystore.p12 and certificate password>"
    echo " 2. argument: <local CA password>"
    exit -1
fi

PASSWORD=${1}
PASSWORD_CA=${2}

echo "Generating key pair"
openssl genrsa -out localhost.key 2048

echo "Generating CSR"
openssl req -newkey rsa:2048 -nodes -keyout localhost.key -sha256 -out localhost.csr -outform PEM -config all-services.ext -extensions v3_req
rm all-services.keystore.key
cp localhost.key all-services.keystore.key

echo "Export local CA keys"
keytool -exportcert -keystore ../local_ca/localca.keystore.p12 -alias localca -storepass ${PASSWORD_CA} -rfc -file local_ca.pem
openssl pkcs12 -in ../local_ca/localca.keystore.p12 -nodes -nocerts -out local_ca.key -legacy -password pass:${PASSWORD_CA}

echo "Signing CSR"
openssl x509 -req -in localhost.csr -CA local_ca.pem -CAkey local_ca.key -CAcreateserial -out localhost.crt -days 1825 -sha256 -extfile all-services.ext -extensions v3_req
rm all-services.keystore.cer
cp localhost.crt all-services.keystore.cer
cat local_ca.pem >> localhost.crt
rm all-services.pem
cat localhost.key > all-services.pem
cat localhost.crt >> all-services.pem

echo "Creating new keystore"
openssl pkcs12 -export -out all-services.keystore.p12 -in localhost.crt -inkey localhost.key -name localhost -macalg SHA256 -password pass:${PASSWORD}

echo "Add local CA into keystore because of issue https://github.com/zowe/api-layer/issues/4420"
keytool -importcert -keystore all-services.keystore.p12 -alias local_ca -file local_ca.pem -noprompt -storepass ${PASSWORD}

echo "Cleaning folder"
rm localhost.crt localhost.key local_ca.srl localhost.csr local_ca.key local_ca.pem

