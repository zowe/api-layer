#!/bin/sh

set -e
set -x
###################################################################
# Regenerates every certificate, keystore and truststore used for local
# development and for the integration tests.
#
# For each certificate an openssl CSR is produced from a committed .ext
# configuration and signed by one of the local authorities, then packaged into a
# PKCS12 keystore. Truststores are assembled with keytool.
#
# Layout is by purpose, not by deployment profile:
#
#   ca/        the three certificate authorities
#   service/   the identities API ML presents, and the default truststore
#   client/    user certificates for client-certificate authentication
#   negative/  certificates that must fail validation, one per failure mode
#   public_ca/ real public roots, not generated - see README.md
#
# Each certificate has its own .ext file next to the keystore it ends up in.
# That .ext file is the single place its DN, key usage and SANs are defined.
#
# Those .ext files are openssl configuration files, and the "v3" in the section
# names refers to X.509 version 3 - the version that introduced extensions such as
# basicConstraints, keyUsage and subjectAltName. openssl is told which section to
# read them from with -extensions, so each file declares exactly one:
#
#   [ v3_ca ]   in ca/*.ext - the authorities. Carries basicConstraints CA:TRUE
#               and keyUsage keyCertSign, which is what makes a certificate able
#               to sign others.
#
#   [ v3_req ]  in every other .ext - the leaf certificates. Carries the
#               extendedKeyUsage (serverAuth / clientAuth) and subjectAltName that
#               decide what each one is allowed to be used for.
#
# The names are the conventional ones from openssl's own openssl.cnf; openssl does
# not attach meaning to them beyond matching what -extensions asks for.
#
# Usage, from the keystore directory:
#
#     ./generate-certificates.sh password local_ca_password
#
# See README.md for what each artifact is for and which DNs must not change.
###################################################################

if [ $# != 2 ]; then
    echo "Arguments were not provided or they are invalid:"
    echo " 1. argument: <keystore and certificate password>"
    echo " 2. argument: <certificate authority password>"
    exit 1
fi

PASSWORD=${1}
PASSWORD_CA=${2}

# Both tools are required. keytool comes with the JDK, which is a prerequisite for
# building this repository at all, but openssl is absent from minimal container
# images - ubuntu:latest, which the CI jobs run in, does not ship it. Check up
# front so the failure names the missing tool instead of surfacing as
# "openssl: not found" from somewhere in the middle of generation.
for tool in openssl keytool; do
    if ! command -v "${tool}" > /dev/null 2>&1; then
        echo "Cannot generate certificates: '${tool}' is not on the PATH." >&2
        echo >&2
        case "${tool}" in
            openssl)
                echo "Install it with your package manager, for example:" >&2
                echo "  apt-get update && apt-get install -y openssl" >&2
                echo "  brew install openssl" >&2
                echo "On Windows it ships with Git for Windows; add its usr/bin" >&2
                echo "directory to the PATH, e.g. C:\\Program Files\\Git\\usr\\bin" >&2
                ;;
            keytool)
                echo "keytool ships with the JDK. Ensure a JDK is installed and that" >&2
                echo "JAVA_HOME/bin is on the PATH." >&2
                ;;
        esac
        exit 1
    fi
done

# Leaf certificates are valid for 5 years, certificate authorities for 10.
DAYS=1825
DAYS_CA=3650

cd "$(dirname "$0")"

WORK=$(mktemp -d)
trap 'rm -rf "${WORK}"' EXIT

###################################################################
# Helpers
###################################################################

# create_ca <name> <password>
#
# Generates a self-signed CA from ca/<name>.ext and writes both the public
# certificate (ca/<name>.cer) and a PKCS12 holding its private key
# (ca/<name>.keystore.p12), using <name> as the alias throughout.
#
# The keystore is written to the working tree but is never committed - see
# .gitignore. Each authority is minted in the same run as the certificates it
# signs, so its private key is always the one that signed them and anything it
# issued can be re-issued.
create_ca() {
    ca_name=${1}
    ca_password=${2}

    echo "Generating certificate authority ca/${ca_name}"
    openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days ${DAYS_CA} \
        -config "ca/${ca_name}.ext" -extensions v3_ca \
        -keyout "${WORK}/${ca_name}.key" -out "${WORK}/${ca_name}.pem"

    cp "${WORK}/${ca_name}.pem" "ca/${ca_name}.cer"

    rm -f "ca/${ca_name}.keystore.p12"
    openssl pkcs12 -export -out "ca/${ca_name}.keystore.p12" \
        -in "${WORK}/${ca_name}.pem" -inkey "${WORK}/${ca_name}.key" \
        -name "${ca_name}" -macalg SHA256 -password "pass:${ca_password}"
}

# sign_cert <ext-file> <work-name> <ca-name>
#
# Generates a key pair and CSR from <ext-file>, then signs the CSR with the given
# CA. Leaves <work-name>.key and <work-name>.crt in the working directory.
#
# <work-name> is deliberately independent of the .ext filename: it must not
# collide with any CA name, or the CA private key in the working directory would
# be overwritten by this certificate's key before signing.
sign_cert() {
    cert_ext=${1}
    cert_name=${2}
    cert_ca=${3}

    echo "Generating CSR for ${cert_name}"
    openssl req -newkey rsa:2048 -nodes -sha256 -outform PEM \
        -config "${cert_ext}" -extensions v3_req \
        -keyout "${WORK}/${cert_name}.key" -out "${WORK}/${cert_name}.csr"

    echo "Signing CSR for ${cert_name}"
    openssl x509 -req -in "${WORK}/${cert_name}.csr" -sha256 -days ${DAYS} \
        -CA "${WORK}/${cert_ca}.pem" -CAkey "${WORK}/${cert_ca}.key" -CAcreateserial \
        -extfile "${cert_ext}" -extensions v3_req \
        -out "${WORK}/${cert_name}.crt"
}

# build_keystore <output> <alias> <work-name> <ca-name>
#
# Packages a signed certificate and its key into a PKCS12 keystore, including the
# CA in the chain. The CA is additionally imported as a trusted entry because of
# https://github.com/zowe/api-layer/issues/4420
build_keystore() {
    ks_out=${1}
    ks_alias=${2}
    ks_name=${3}
    ks_ca=${4}

    echo "Creating keystore ${ks_out}"
    cat "${WORK}/${ks_name}.crt" "${WORK}/${ks_ca}.pem" > "${WORK}/${ks_name}.chain"

    rm -f "${ks_out}"
    openssl pkcs12 -export -out "${ks_out}" \
        -in "${WORK}/${ks_name}.chain" -inkey "${WORK}/${ks_name}.key" \
        -name "${ks_alias}" -macalg SHA256 -password "pass:${PASSWORD}"

    keytool -importcert -keystore "${ks_out}" -storetype PKCS12 \
        -alias "${ks_ca}" -file "${WORK}/${ks_ca}.pem" -noprompt -storepass "${PASSWORD}" > /dev/null
}

# import_trusted <truststore> <alias> <certificate>
import_trusted() {
    keytool -importcert -keystore "${1}" -storetype PKCS12 \
        -alias "${2}" -file "${3}" -noprompt -storepass "${PASSWORD}" > /dev/null
}

###################################################################
# Certificate authorities
#
# Three, each named for what it signs and each with a distinct subject DN.
###################################################################

mkdir -p ca service client negative public_ca

create_ca service-ca   "${PASSWORD_CA}"
create_ca client-ca    "${PASSWORD}"
create_ca untrusted-ca "${PASSWORD_CA}"

###################################################################
# Service identity
#
# One certificate presented by every service in every profile. Its SAN list is the
# union of every hostname API ML is reached by - plain localhost, the multi-instance
# local profile, and every container hostname - so no profile needs its own.
###################################################################

sign_cert service/service.ext service service-ca
build_keystore service/service.keystore.p12 localhost service service-ca

# Convenience exports, consumed by the Node.js and Python enablers, the
# OpenTelemetry collector, the API Catalog UI dev server and the ZSS sample.
cp "${WORK}/service.crt" service/service.cer
cp "${WORK}/service.key" service/service.key
cat "${WORK}/service.crt" ca/service-ca.cer > service/service.pem

###################################################################
# Split-role identities
#
# The serverAuth / clientAuth split is deliberate: config/docker/*.yml uses
# server-only.p12 for the listener and client-cert.p12 for outbound calls, so a
# listener certificate cannot be replayed as a client identity.
###################################################################

sign_cert service/server-only.ext server-only service-ca
build_keystore service/server-only.p12 localhost server-only service-ca

sign_cert service/client-cert.ext client-cert service-ca
build_keystore service/client-cert.p12 localhost client-cert service-ca

###################################################################
# Client certificates for client-certificate authentication
#
# Three user identities in one keystore, selected by alias at connection time.
###################################################################

rm -f client/client-certs.p12
for user in apimtst user unknownuser; do
    sign_cert "client/client-${user}.ext" "client-${user}" client-ca

    cat "${WORK}/client-${user}.crt" "${WORK}/client-ca.pem" > "${WORK}/client-${user}.chain"
    openssl pkcs12 -export -out "${WORK}/client-${user}.p12" \
        -in "${WORK}/client-${user}.chain" -inkey "${WORK}/client-${user}.key" \
        -name "${user}" -macalg SHA256 -password "pass:${PASSWORD}"

    echo "Adding ${user} to client/client-certs.p12"
    keytool -importkeystore -noprompt \
        -srckeystore "${WORK}/client-${user}.p12" -srcstoretype PKCS12 -srcstorepass "${PASSWORD}" \
        -destkeystore client/client-certs.p12 -deststoretype PKCS12 -deststorepass "${PASSWORD}" \
        > /dev/null 2>&1
done

###################################################################
# Negative-test certificates
#
# Three distinct failure modes, one certificate each.
###################################################################

# Valid chain, hostname does not match.
sign_cert negative/hostname-mismatch.ext hostname-mismatch service-ca
build_keystore negative/hostname-mismatch.keystore.p12 nonlocalhost hostname-mismatch service-ca

# Chain rooted in an authority that is not in the default truststore. The work
# name must differ from the CA name - see the note on sign_cert.
sign_cert negative/untrusted-ca.ext untrusted-leaf untrusted-ca
build_keystore negative/untrusted-ca.keystore.p12 localhost untrusted-leaf untrusted-ca

# Self-signed, no issuer at all.
echo "Generating self-signed certificate negative/selfsigned"
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days ${DAYS} \
    -config negative/selfsigned.ext -extensions v3_req \
    -keyout "${WORK}/selfsigned.key" -out "${WORK}/selfsigned.crt"

rm -f negative/selfsigned.keystore.p12
openssl pkcs12 -export -out negative/selfsigned.keystore.p12 \
    -in "${WORK}/selfsigned.crt" -inkey "${WORK}/selfsigned.key" \
    -name localhost -macalg SHA256 -password "pass:${PASSWORD}"
cp "${WORK}/selfsigned.crt" negative/selfsigned.cer
cp "${WORK}/selfsigned.key" negative/selfsigned.key

###################################################################
# Truststores
###################################################################

# The single default truststore, used by every service and by the integration
# tests: the authority that signs service certificates plus the authority that
# signs client certificates, and nothing else. Public roots live separately in
# public_ca, so this store holds only what this repository issues.
echo "Creating truststore service/service.truststore.p12"
rm -f service/service.truststore.p12
import_trusted service/service.truststore.p12 service-ca ca/service-ca.cer
import_trusted service/service.truststore.p12 client-ca  ca/client-ca.cer

# The public anchors are merged in as well, because API ML validates live HTTPS
# endpoints with this truststore rather than with the JVM default one - the OIDC
# support fetches a JWKS over TLS through HttpConfig.getSecureSslContextWithoutKeystore
# (see HttpsJwksProvider), so the authority behind that endpoint has to be in here.
#
# public_ca/public-roots.p12 stays the maintained source for those anchors: it holds
# real third-party certificates that are not generated and expire on their own
# schedule. See README.md for how to refresh it.
echo "Merging public_ca/public-roots.p12 into service/service.truststore.p12"
keytool -importkeystore -noprompt \
    -srckeystore public_ca/public-roots.p12 -srcstoretype PKCS12 -srcstorepass "${PASSWORD}" \
    -destkeystore service/service.truststore.p12 -deststoretype PKCS12 -deststorepass "${PASSWORD}" \
    > /dev/null 2>&1

# Negative truststore: only the untrusted authority, so the default service
# certificate fails path validation against it.
echo "Creating truststore negative/untrusted-ca.truststore.p12"
rm -f negative/untrusted-ca.truststore.p12
import_trusted negative/untrusted-ca.truststore.p12 untrusted-ca ca/untrusted-ca.cer

# PEM bundle of both trusted authorities, for the NGINX proxy simulating AT-TLS.
cat ca/service-ca.cer ca/client-ca.cer > service/trusted_CAs.cer

###################################################################
# Module-local copies
###################################################################

# common-service-core asserts that it can find the service certificate's private
# key in the keystore given only the matching public key, read from this file. It
# is derived from the service certificate, so it has to be refreshed with it.
echo "Refreshing common-service-core jwt-public-key.pub"
openssl x509 -in service/service.cer -noout -pubkey \
    | grep -v -- '-----' \
    > ../common-service-core/src/test/resources/jwt-public-key.pub

ZAAS_RES=../zaas-client/src/test/resources

# zaas-client resolves its stores by relative path (src/test/resources/...) rather
# than from the classpath, so it needs its own copy rather than reading keystore/
# directly. Writing it from here keeps this script the single source of truth.
echo "Refreshing zaas-client test copies"
cp service/service.keystore.p12 "${ZAAS_RES}/localhost.keystore.p12"

# The zaas-client truststore is the project truststore plus one extra anchor: the
# self-signed CA that MockServer presents for its own HTTPS listener. It is scoped
# to this module rather than added to the shared truststore, because only these
# tests talk to a MockServer over TLS. MockServer cannot be pointed at our own
# certificate without also handing it our CA private key in PEM form.
rm -f "${ZAAS_RES}/localhost.truststore.p12"
import_trusted "${ZAAS_RES}/localhost.truststore.p12" service-ca ca/service-ca.cer
import_trusted "${ZAAS_RES}/localhost.truststore.p12" client-ca  ca/client-ca.cer
import_trusted "${ZAAS_RES}/localhost.truststore.p12" www.mockserver.com "${ZAAS_RES}/mockserver-ca.cer"

###################################################################
# File permissions
#
# openssl creates private keys readable only by their owner. That breaks the
# containers which bind-mount this directory and run as an unprivileged user: the
# OpenTelemetry collector reads service/service.key directly and fails to start
# with "permission denied" on an owner-only file owned by the build user.
#
# 0644 is also the mode these files had while they were committed, because git
# records only the executable bit. There is nothing here to protect - they are
# development certificates whose passwords are published in this repository.
#
# Done with shell globbing rather than find(1) on purpose. When Gradle launches this
# from PowerShell, the inherited PATH has C:\Windows\System32 ahead of Git's usr/bin,
# and System32 contains an unrelated find.exe that searches files for a string. It
# rejects these arguments with "File not found". find is the only command used here
# whose name collides with a different Windows tool.
###################################################################

echo "Relaxing permissions for use inside containers"
relax_permissions() {
    for file in "$@"; do
        [ -f "${file}" ] || continue
        chmod 644 "${file}"
    done
}

for directory in ca service client negative public_ca; do
    relax_permissions "${directory}"/*
done
relax_permissions "${ZAAS_RES}/localhost.keystore.p12" \
                  "${ZAAS_RES}/localhost.truststore.p12" \
                  ../common-service-core/src/test/resources/jwt-public-key.pub

###################################################################
# Verification
#
# Every chain is checked here rather than being discovered by a failing test.
###################################################################

echo
echo "Verifying"

# Each CA keystore must hold the same certificate published as .cer next to it.
# If they diverge, the authority's private key cannot re-issue the certificates
# bearing that .cer as their issuer, and the mismatch is invisible until something
# tries to sign with it.
verify_ca_pair() {
    ca_name=${1}
    ca_password=${2}

    published=$(openssl x509 -in "ca/${ca_name}.cer" -noout -fingerprint -sha256)
    stored=$(keytool -exportcert -rfc -keystore "ca/${ca_name}.keystore.p12" \
        -storepass "${ca_password}" -alias "${ca_name}" 2>/dev/null \
        | openssl x509 -noout -fingerprint -sha256)
    if [ "${published}" = "${stored}" ] && [ -n "${stored}" ]; then
        echo "  ok      ${ca_name} key pairing"
    else
        echo "  FAILED  ${ca_name} key pairing - keystore does not hold the published certificate"
        exit 1
    fi
}

verify_chain() {
    label=${1}
    ca_name=${2}
    keystore=${3}
    alias_name=${4}

    keytool -exportcert -rfc -keystore "${keystore}" -storepass "${PASSWORD}" \
        -alias "${alias_name}" > "${WORK}/verify.pem" 2> /dev/null
    if openssl verify -CAfile "ca/${ca_name}.cer" "${WORK}/verify.pem" > /dev/null 2>&1; then
        echo "  ok      ${label}"
    else
        echo "  FAILED  ${label}"
        exit 1
    fi
}

verify_ca_pair service-ca   "${PASSWORD_CA}"
verify_ca_pair client-ca    "${PASSWORD}"
verify_ca_pair untrusted-ca "${PASSWORD_CA}"

verify_chain "service identity"         service-ca   service/service.keystore.p12            localhost
verify_chain "serverAuth-only"          service-ca   service/server-only.p12                 localhost
verify_chain "clientAuth-only"          service-ca   service/client-cert.p12                 localhost
verify_chain "hostname mismatch"        service-ca   negative/hostname-mismatch.keystore.p12 nonlocalhost
verify_chain "untrusted CA leaf"        untrusted-ca negative/untrusted-ca.keystore.p12      localhost
verify_chain "client cert APIMTST"      client-ca    client/client-certs.p12                 apimtst
verify_chain "client cert USER"         client-ca    client/client-certs.p12                 user
verify_chain "client cert UNKNOWNUSER"  client-ca    client/client-certs.p12                 unknownuser

# The untrusted leaf must NOT validate against the service CA, otherwise the
# "different certificate authority should fail" tests pass for the wrong reason.
keytool -exportcert -rfc -keystore negative/untrusted-ca.keystore.p12 \
    -storepass "${PASSWORD}" -alias localhost > "${WORK}/negative.pem" 2> /dev/null
if openssl verify -CAfile ca/service-ca.cer "${WORK}/negative.pem" > /dev/null 2>&1; then
    echo "  FAILED  untrusted leaf must not chain to the service CA"
    exit 1
fi
echo "  ok      untrusted leaf is rejected by the service CA"

echo
echo "Done."
