#!/usr/bin/env bash
#
# generate-keystores.sh — Generate all TLS keystores for local development and testing.
#
# This script creates the complete set of PKCS12 keystores, truststores,
# and convenience exports needed to run the Zowe API Mediation Layer locally.
# It requires openssl and keytool to be available on the PATH.
#
# Usage:
#   ./scripts/generate-keystores.sh
#
# All generated files are written under keystore/ (relative to the repo root).

set -euo pipefail

# ── Prerequisite detection ─────────────────────────────────────────────────
# Find keytool: try JAVA_HOME, then sdkman candidates, then PATH
find_keytool() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
        echo "$JAVA_HOME/bin/keytool"
        return
    fi
    # Check sdkman Java 17
    for sdk_java in "$HOME/.sdkman/candidates/java/"{17.*,21.*,current}; do
        if [ -x "$sdk_java/bin/keytool" ]; then
            echo "$sdk_java/bin/keytool"
            return
        fi
    done
    # Fall back to PATH
    if command -v keytool &>/dev/null; then
        echo "keytool"
        return
    fi
    echo ""
}

KEYTOOL="$(find_keytool)"
if [ -z "$KEYTOOL" ]; then
    echo "ERROR: keytool not found. Set JAVA_HOME or ensure Java is installed." >&2
    exit 1
fi

# Resolve the Java bin directory. When keytool was found on PATH, dirname would yield "."
# and prepending that to PATH would put the working directory on the search path.
if [ "$KEYTOOL" = "keytool" ]; then
    JAVA_BIN="$(dirname "$(command -v keytool)")"
else
    JAVA_BIN="$(dirname "$KEYTOOL")"
    export PATH="$JAVA_BIN:$PATH"
fi

if ! command -v openssl &>/dev/null; then
    echo "ERROR: openssl not found. Please install openssl." >&2
    exit 1
fi

# Report the JDK via `java -version`: keytool has no -version option and prints its
# entire usage text when given one, which buries the rest of the build output.
JAVA_VERSION=""
if [ -x "$JAVA_BIN/java" ]; then
    JAVA_VERSION="$("$JAVA_BIN/java" -version 2>&1 || true)"
    JAVA_VERSION="${JAVA_VERSION%%$'\n'*}"
fi
echo "Using keytool: $KEYTOOL${JAVA_VERSION:+ ($JAVA_VERSION)}"
echo "Using openssl: $(openssl version)"

# Find Java cacerts for importing public CA certificates. KEYTOOL is already resolved,
# so derive the JDK root from it rather than scanning the filesystem.
find_cacerts() {
    local java_home="${JAVA_HOME:-$(cd "$JAVA_BIN/.." && pwd)}"
    [ -z "$java_home" ] && return
    for candidate in "$java_home/lib/security/cacerts" "$java_home/jre/lib/security/cacerts"; do
        if [ -f "$candidate" ]; then
            echo "$candidate"
            return
        fi
    done
}

JAVA_CACERTS="$(find_cacerts)"
if [ -n "$JAVA_CACERTS" ]; then
    echo "Found Java cacerts: $JAVA_CACERTS"
else
    echo "WARNING: Could not find Java cacerts truststore. External TLS connections may fail."
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="$SCRIPT_DIR/$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYSTORE_DIR="$REPO_ROOT/keystore"
# Records which revision of this script produced the tree; see the Gradle task, which
# regenerates when the checksum no longer matches the script it is about to run.
STAMP_FILE="$KEYSTORE_DIR/generation.stamp"

# ── Passwords ──────────────────────────────────────────────────────────────
PASSWORD="password"
CA_PASSWORD="local_ca_password"

# ── Shared helpers ─────────────────────────────────────────────────────────
# Generate a self-signed CA.
#
# The extensions come from a config file rather than from `-addext`, for portability:
# LibreSSL (the openssl that ships with macOS) rejects `authorityKeyIdentifier=none`
# outright, and OpenSSL 3 only auto-adds an authorityKeyIdentifier when no extension
# section is supplied — so handing it a section is what keeps the AKI off the CA
# certificate. A self-referential AKI on the root breaks `keytool -gencert` chains.
generate_ca() {
    local key_file="$1"    # private key to create
    local out_pem="$2"     # certificate to create
    local dn_block="$3"    # distinguished name, one "Field = value" per line

    local ca_config
    ca_config=$(mktemp)
    cat > "$ca_config" << EOF
[req]
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_ca

[dn]
$dn_block

[v3_ca]
basicConstraints = critical,CA:TRUE
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
EOF

    openssl genrsa -out "$key_file" 2048
    openssl req -x509 -new -nodes -key "$key_file" -sha256 -days 3650 \
        -out "$out_pem" -config "$ca_config" -extensions v3_ca
    rm -f "$ca_config"
}

# Public CA certificates that the generated truststores must trust: OIDC providers and
# the other external endpoints the tests reach. Importing all JDK cacerts (~150 certs)
# bloats the truststore and causes startup timing regressions in CI, so only these are
# pulled in.
#
# Roots are looked up by subject, not by the JDK's alias: alias spelling differs between
# JDK vendors and versions, so an alias list silently imports nothing on a JDK that names
# them differently — leaving a truststore with no public roots and no error to show it.
WANTED_PUBLIC_CAS=(
    "DigiCert Global Root CA"
    "DigiCert Global Root G2"
    "DigiCert Global Root G3"
    "ISRG Root X1"
    "GTS Root R1"
    "GTS Root R2"
    "GTS Root R3"
    "GTS Root R4"
)

# "<subject>\t<alias>" for every entry in the JDK truststore; built on first use.
CACERTS_INDEX=""
build_cacerts_index() {
    [ -n "$CACERTS_INDEX" ] && return 0
    CACERTS_INDEX="$(keytool -list -v -keystore "$JAVA_CACERTS" -storepass changeit 2>/dev/null | awk '
        /^Alias name: / { alias = substr($0, 13) }
        /^Owner: /      { print substr($0, 8) "\t" alias }')"
}

# Resolve a CA common name to its alias in the JDK truststore. Compares whole RDNs so
# that "GTS Root R1" cannot match "GTS Root R11".
cacerts_alias_for() {
    printf '%s\n' "$CACERTS_INDEX" | awk -F'\t' -v want="CN=$1" '
        {
            n = split($1, rdns, ", ")
            for (i = 1; i <= n; i++) if (rdns[i] == want) { print $2; exit }
        }'
}

import_cacerts() {
    local truststore="$1"
    if [ -z "$JAVA_CACERTS" ] || [ ! -f "$JAVA_CACERTS" ]; then
        echo "WARNING: Java cacerts not found; skipping public CA import into $truststore"
        return
    fi
    build_cacerts_index
    echo "Importing required public CA certificates into $truststore"
    local ca alias tmpcert imported=0
    for ca in "${WANTED_PUBLIC_CAS[@]}"; do
        alias="$(cacerts_alias_for "$ca")"
        if [ -z "$alias" ]; then
            echo "WARNING: '$ca' not found in $JAVA_CACERTS; TLS connections that rely on it will fail"
            continue
        fi
        tmpcert=$(mktemp)
        if keytool -exportcert -keystore "$JAVA_CACERTS" -storepass changeit \
                -alias "$alias" -file "$tmpcert" 2>/dev/null &&
           keytool -importcert -keystore "$truststore" -storetype pkcs12 \
                -storepass "$PASSWORD" -alias "$ca" -file "$tmpcert" -noprompt 2>/dev/null; then
            imported=$((imported + 1))
        else
            echo "WARNING: failed to import '$ca' (alias '$alias') into $truststore"
        fi
        rm -f "$tmpcert"
    done
    echo "  imported $imported of ${#WANTED_PUBLIC_CAS[@]} public CA certificates"
}

# Trust everything dropped into keystore/extra_ca. This is the supported way to restore
# CAs that used to live inside the committed truststores and cannot be rebuilt from
# public material — corporate/internal roots needed to reach an internal z/OSMF or OIDC
# provider. Drop the PEM in and re-run; nothing internal is committed to the repository.
import_extra_cas() {
    local truststore="$1"
    local extra_dir="$KEYSTORE_DIR/extra_ca"
    # `return 0`, not a bare `return`: under `set -e` a bare one would propagate the
    # failed test and abort the whole script when the directory is simply absent.
    [ -d "$extra_dir" ] || return 0
    local cert alias
    for cert in "$extra_dir"/*.pem "$extra_dir"/*.cer "$extra_dir"/*.crt; do
        [ -f "$cert" ] || continue
        alias="extra_$(basename "${cert%.*}")"
        echo "Importing extra CA $(basename "$cert") into $truststore"
        keytool -importcert -keystore "$truststore" -storetype pkcs12 \
            -storepass "$PASSWORD" -alias "$alias" -file "$cert" -noprompt
    done
}

# Every truststore gets the same external trust material.
finalize_truststore() {
    import_cacerts "$1"
    import_extra_cas "$1"
}

# ── Clean up previous generated artifacts ──────────────────────────────────
echo "=== Cleaning previously generated keystores ==="
for dir in local_ca localhost selfsigned docker; do
    if [ -d "$KEYSTORE_DIR/$dir" ]; then
        find "$KEYSTORE_DIR/$dir" -type f \( -name '*.p12' -o -name '*.cer' -o -name '*.key' -o -name '*.pem' -o -name '*.csr' -o -name '*.crt' -o -name '*.srl' \) -delete 2>/dev/null || true
    fi
done
# Also clean client_cert generated files (keep openssl.conf)
find "$KEYSTORE_DIR/client_cert" -type f \( -name '*.p12' -o -name '*.cer' -o -name '*.key' -o -name '*.pem' -o -name '*.csr' -o -name '*.crt' -o -name '*.srl' \) -delete 2>/dev/null || true

mkdir -p "$KEYSTORE_DIR/local_ca" "$KEYSTORE_DIR/localhost" "$KEYSTORE_DIR/selfsigned" "$KEYSTORE_DIR/docker" "$KEYSTORE_DIR/client_cert/ca"

# ── 1. Local Certificate Authority ────────────────────────────────────────
echo ""
echo "=== Generating Local Certificate Authority ==="
cd "$KEYSTORE_DIR/local_ca"

# Generate the CA key and self-signed certificate. The distinguished name reproduces the
# one the committed keystores carried: config/local/gateway-service.yml authorizes the
# x509 registry by certificate common name, and docker/redis/run-redis.sh builds its own
# CA with this exact DN.
generate_ca local_ca.key local_ca.pem "C = CZ
ST = Prague
L = Prague
O = Zowe Sample
OU = API Mediation Layer
CN = Zowe Development Instances Certificate Authority"

# Create PKCS12 keystore for the CA
openssl pkcs12 -export -out localca.keystore.p12 \
    -in local_ca.pem -inkey local_ca.key \
    -name localca -macalg SHA256 -passout "pass:$CA_PASSWORD"

# Export public certificate (DER format)
openssl x509 -in local_ca.pem -outform DER -out localca.cer
cp localca.cer zowe-dev-ca.cer

# Keep PEM copy for signing operations in later steps
cp local_ca.pem localca.pem

# Export to localhost directory for convenience (used by NGINX AT-TLS)
cp localca.cer "$KEYSTORE_DIR/localhost/localca.cer"
cp localca.cer "$KEYSTORE_DIR/localhost/Zowe_Service_Zowe_Development_Instances_Certificate_Authority_.cer"

# Second, intentionally unrelated CA. It signs the whole localhost2 pair — keystore and
# truststore — so that localhost2 is a self-consistent PKI that has nothing in common with
# the main one. Both directions are load-bearing in TomcatHttpsTest:
#   trustStoreWithDifferentCertificateAuthorityShouldFail — a client trusting only this CA
#       must reject a server holding the main CA's certificate, and
#   wrongClientCertificateShouldNotFailWhenClientAuthIsWant — the localhost2 certificate
#       must be an untrusted client certificate as far as the main truststore is concerned.
# Signing localhost2 with the main CA instead leaves the second test asserting nothing.
echo ""
echo "=== Generating Secondary Certificate Authority (for truststore mismatch tests) ==="
generate_ca local_ca2.key localca2.pem "C = CZ
ST = Prague
L = Prague
O = Zowe Sample
OU = API Mediation Layer
CN = Zowe Development Instances Certificate Authority 2"
openssl x509 -in localca2.pem -outform DER -out localca2.cer

# ── 2. Localhost keystores ─────────────────────────────────────────────────
echo ""
echo "=== Generating localhost keystores ==="
cd "$KEYSTORE_DIR/localhost"

# Default SAN block for a certificate that has to answer for the local machine.
# 127.0.0.1 is listed as an IP entry, not a DNS one: a DNS name that happens to look like
# an address is not what a hostname verifier consults when the URL holds an IP literal.
LOCALHOST_SANS="DNS.1 = localhost
DNS.2 = localhost.localdomain
IP.1 = 127.0.0.1"

generate_localhost_keystore() {
    local name="$1"        # base name for the intermediate files
    local alias="$2"       # alias of the key entry in the keystore
    local ks_name="$3"     # output keystore filename
    local ts_name="$4"     # output truststore filename
    local ca_cert="$5"     # CA certificate: signs the key entry, trusted by the truststore
    local ca_key="$6"      # CA private key (unencrypted, next to the PEM)
    local dn_block="$7"    # subject, one "Field = value" per line
    local san_entries="$8" # SAN block; LOCALHOST_SANS for anything serving localhost

    # Generate server key and CSR
    local san_config
    san_config=$(mktemp)
    cat > "$san_config" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
$dn_block

[v3_req]
keyUsage = digitalSignature, nonRepudiation, keyEncipherment
extendedKeyUsage = clientAuth, serverAuth
subjectAltName = @alt_names

[alt_names]
$san_entries
EOF

    # Generate private key
    openssl genrsa -out "${name}.key" 2048

    # Generate CSR
    openssl req -new -key "${name}.key" -out "${name}.csr" -config "$san_config"

    # Sign with CA
    openssl x509 -req -in "${name}.csr" \
        -CA "$ca_cert" -CAkey "$ca_key" \
        -out "${name}.crt" -days 3650 -sha256 \
        -extfile "$san_config" -extensions v3_req -CAcreateserial

    # Create truststore with CA cert
    keytool -import -alias localca -file "$ca_cert" \
        -keystore "$ts_name" -storetype pkcs12 -storepass "$PASSWORD" -noprompt

    # Create PKCS12 keystore with server cert + key
    openssl pkcs12 -export -out "$ks_name" \
        -in "${name}.crt" -inkey "${name}.key" \
        -name "$alias" -macalg SHA256 -passout "pass:$PASSWORD"

    # Import CA into keystore
    keytool -importcert -keystore "$ks_name" -alias localca \
        -file "$ca_cert" -noprompt -storepass "$PASSWORD" -storetype pkcs12

    # Clean up
    rm -f "${name}.key" "${name}.csr" "${name}.crt" "$san_config"
}

# Subject shared by the service certificates. CN=Zowe Service is what
# config/local/gateway-service.yml lists in apiml.security.x509.registry.allowedUsers,
# which is matched against the client certificate's common name.
ZOWE_SERVICE_DN="C = CZ
ST = Prague
L = Prague
O = Zowe Sample
OU = API Mediation Layer
CN = Zowe Service"

# --- localhost standard ---
generate_localhost_keystore \
    "localhost" "localhost" \
    "localhost.keystore.p12" "localhost.truststore.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" \
    "$ZOWE_SERVICE_DN" "$LOCALHOST_SANS"

# Export convenience files
keytool -exportcert -keystore localhost.keystore.p12 -alias localhost \
    -storepass "$PASSWORD" -storetype pkcs12 -rfc -file localhost.pem
openssl pkcs12 -in localhost.keystore.p12 -nocerts -nodes \
    -passin "pass:$PASSWORD" -out localhost.keystore.key
# Export certificate in PEM format (OpenTelemetry collector requires PEM)
openssl pkcs12 -in localhost.keystore.p12 -passin "pass:$PASSWORD" -nokeys 2>/dev/null \
    | openssl x509 -outform PEM -out localhost.keystore.cer 2>/dev/null || true

# --- localhost2: signed by the secondary CA, and its truststore trusts only that CA ---
generate_localhost_keystore \
    "localhost2" "localhost" \
    "localhost2.keystore.p12" "localhost2.truststore.p12" \
    "../local_ca/localca2.pem" "../local_ca/local_ca2.key" \
    "C = CZ
ST = Prague
L = Prague
O = Zowe Sample
OU = API Mediation Layer
CN = Zowe Service 2" "$LOCALHOST_SANS"

# --- nonlocalhost ---
# Intentionally does NOT match "localhost". This keystore exists to prove that strict
# hostname verification rejects a service whose certificate is for another host, so a
# SAN entry for localhost here silently disables the tests that depend on it.
generate_localhost_keystore \
    "nonlocalhost.local" "nonlocalhost" \
    "nonlocalhost.keystore.p12" "does-not-matter.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" \
    "C = CZ
ST = Czechia
L = Prague
O = Broadcom
OU = MSD
CN = nonlocalhost.local" \
    "DNS.1 = nonlocalhost.local"
rm -f does-not-matter.p12

# --- localhost-multi ---
# Serves the multi-instance local setup, where config/local-multi addresses the second
# instance as https://localhost2:10021 — so the certificate has to answer for localhost2
# and localhost3 as well, not just localhost.
generate_localhost_keystore \
    "localhost-multi" "localhost-multi" \
    "localhost-multi.keystore.p12" "localhost-multi.truststore.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" \
    "$ZOWE_SERVICE_DN" \
    "$LOCALHOST_SANS
DNS.3 = localhost2
DNS.4 = localhost3"

# Trusted CAs chain for NGINX proxy
cat ../local_ca/localca.cer > trusted_CAs.cer

for ts in localhost.truststore.p12 localhost2.truststore.p12 localhost-multi.truststore.p12; do
    finalize_truststore "$ts"
done

# Import mockserver certificate for zaas-client tests
# mockserver-netty presents this cert during TLS negotiation
MOCKSERVER_TMP=$(mktemp)
cat > "$MOCKSERVER_TMP" << 'MOCKSERVER_CERT'
-----BEGIN CERTIFICATE-----
MIIDqDCCApCgAwIBAgIEPhwe6TANBgkqhkiG9w0BAQsFADBiMRswGQYDVQQDDBJ3
d3cubW9ja3NlcnZlci5jb20xEzARBgNVBAoMCk1vY2tTZXJ2ZXIxDzANBgNVBAcM
BkxvbmRvbjEQMA4GA1UECAwHRW5nbGFuZDELMAkGA1UEBhMCVUswIBcNMTYwNjIw
MTYzNDE0WhgPMjExNzA1MjcxNjM0MTRaMGIxGzAZBgNVBAMMEnd3dy5tb2Nrc2Vy
dmVyLmNvbTETMBEGA1UECgwKTW9ja1NlcnZlcjEPMA0GA1UEBwwGTG9uZG9uMRAw
DgYDVQQIDAdFbmdsYW5kMQswCQYDVQQGEwJVSzCCASIwDQYJKoZIhvcNAQEBBQAD
ggEPADCCAQoCggEBAPGORrdkwTY1H1dvQPYaA+RpD+pSbsvHTtUSU6H7NQS2qu1p
sE6TEG2fE+Vb0QIXkeH+jjKzcfzHGCpIU/0qQCu4RVycrIW4CCdXjl+T3L4C0I3R
mIMciTig5qcAvY9P5bQAdWDkU36YGrCjGaX3QlndGxD9M974JdpVK4cqFyc6N4gA
Onys3uS8MMmSHTjTFAgR/WFeJiciQnal+Zy4ZF2x66CdjN+hP8ch2yH/CBwrSBc0
ZeH2flbYGgkh3PwKEqATqhVa+mft4dCrvqBwGhBTnzEGWK/qrl9xB4mTs4GQ/Z5E
8rXzlvpKzVJbfDHfqVzgFw4fQFGV0XMLTKyvOX0CAwEAAaNkMGIwHQYDVR0OBBYE
FH3W3sL4XRDM/VnRayaSamVLISndMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQD
AgG2MCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG
9w0BAQsFAAOCAQEAecfgKuMxCBe/NxVqoc4kzacf9rjgz2houvXdZU2UDBY3hCs4
MBbM7U9Oi/3nAoU1zsA8Rg2nBwc76T8kSsfG1TK3iJkfGIOVjcwOoIjy3Z8zLM2V
YjYbOUyAQdO/s2uShAmzzjh9SV2NKtcNNdoE9e6udvwDV8s3NGMTUpY5d7BHYQqV
sqaPGlsKi8dN+gdLcRbtQo29bY8EYR5QJm7QJFDI1njODEnrUjjMvWw2yjFlje59
j/7LBRe2wfNmjXFYm5GqWft10UJ7Ypb3XYoGwcDac+IUvrgmgTHD+E3klV3SUi8i
Gm5MBedhPkXrLWmwuoMJd7tzARRHHT6PBH/ZGw==
-----END CERTIFICATE-----
MOCKSERVER_CERT

keytool -importcert -keystore localhost.truststore.p12 \
    -storetype pkcs12 -storepass "$PASSWORD" \
    -alias "www.mockserver.com" -file "$MOCKSERVER_TMP" -noprompt
rm -f "$MOCKSERVER_TMP"

# ── 3. Self-signed keystores ───────────────────────────────────────────────
echo ""
echo "=== Generating self-signed keystores ==="
cd "$KEYSTORE_DIR/selfsigned"

generate_selfsigned_keystore() {
    local name="$1"        # base name for the intermediate files
    local alias="$2"       # alias of the key entry in the keystore
    local ks_name="$3"
    local ts_name="$4"
    local subject="$5"     # subject in openssl -subj form

    # Generate self-signed cert (no CA)
    openssl req -x509 -newkey rsa:2048 -nodes -keyout "${name}.key" \
        -out "${name}.crt" -days 3650 -sha256 \
        -subj "$subject" \
        -addext "subjectAltName=DNS:localhost,DNS:localhost.localdomain,IP:127.0.0.1" \
        -addext "keyUsage=digitalSignature,nonRepudiation,keyEncipherment" \
        -addext "extendedKeyUsage=clientAuth,serverAuth"

    # Create keystore
    openssl pkcs12 -export -out "$ks_name" \
        -in "${name}.crt" -inkey "${name}.key" \
        -name "$alias" -macalg SHA256 -passout "pass:$PASSWORD"

    # Create truststore with self-signed cert
    keytool -import -alias localca -file "${name}.crt" \
        -keystore "$ts_name" -storetype pkcs12 -storepass "$PASSWORD" -noprompt

    rm -f "${name}.key" "${name}.crt"
}

# Standard self-signed
generate_selfsigned_keystore "localhost" "localhost" \
    "localhost.keystore.p12" "localhost.truststore.p12" \
    "/C=CZ/ST=Prague/L=Prague/O=Zowe Sample/OU=API Mediation Layer/CN=Zowe Service"

# Untrusted self-signed (different CA, not trusted by any API ML service)
generate_selfsigned_keystore "localhost-untrusted" "localhost" \
    "localhost-untrusted.keystore.p12" "does-not-matter.p12" \
    "/C=CZ/ST=Brno/L=Brno/O=Zowe Sample/OU=API Mediation Layer/CN=Zowe Self-Signed Untrusted Service"
rm -f does-not-matter.p12

# Create an untrusted truststore from a different self-signed CA
generate_ca untrusted_ca.key untrusted_ca.crt "C = CZ
ST = Czechia
L = Prague
O = Untrusted
OU = IT
CN = Untrusted CA"
keytool -import -alias localca -file untrusted_ca.crt \
    -keystore "localhost-untrusted.truststore.p12" \
    -storetype pkcs12 -storepass "$PASSWORD" -noprompt
rm -f untrusted_ca.key untrusted_ca.crt

# ── 4. Docker keystores ────────────────────────────────────────────────────
echo ""
echo "=== Generating Docker keystores ==="
cd "$KEYSTORE_DIR/docker"

# Re-generate local CA PEM files for signing (needed by docker generator)
openssl pkcs12 -in ../local_ca/localca.keystore.p12 -nocerts -nodes \
    -passin "pass:$CA_PASSWORD" -out local_ca.key
keytool -exportcert -keystore ../local_ca/localca.keystore.p12 -alias localca \
    -storepass "$CA_PASSWORD" -rfc -file local_ca.pem

# Generate all-services cert with extended SANs for Docker service names
san_config=$(mktemp)
cat > "$san_config" << 'SANEOF'
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn

[v3_req]
extendedKeyUsage = clientAuth, serverAuth
subjectAltName = @alt_names

[dn]
C = CZ
ST = Czechia
L = Prague
O = Broadcom
OU = MSD
CN = Zowe Component

[alt_names]
DNS.1 = localhost
DNS.2 = 127.0.0.1
DNS.3 = zaas-service
DNS.4 = zaas-service-2
DNS.5 = api-catalog-services
DNS.6 = api-catalog-services-2
DNS.7 = caching-service
DNS.8 = caching-service-2
DNS.9 = caching-service-3
DNS.10 = discovery-service
DNS.11 = discovery-service-2
DNS.12 = discoverable-client
DNS.13 = discoverable-client-1
DNS.14 = discoverable-client-2
DNS.15 = discoverable-client-3
DNS.16 = discoverable-client-4
DNS.17 = discoverable-client-unknown
DNS.18 = mock-services
DNS.19 = mock-services-2
DNS.20 = mock-services-unknown
DNS.21 = reverse-proxy
DNS.22 = gateway-service
DNS.23 = gateway-service-2
DNS.24 = central-gateway-service
DNS.25 = central-gateway-service-2
DNS.26 = apiml
DNS.27 = apiml-2
DNS.28 = apiml-3
DNS.29 = nodejs-sample-app
DNS.30 = python-sample-app
SANEOF

# Generate key and CSR
openssl genrsa -out all-services.key 2048
openssl req -new -key all-services.key -out all-services.csr -config "$san_config"

# Sign with local CA
openssl x509 -req -in all-services.csr \
    -CA local_ca.pem -CAkey local_ca.key \
    -out all-services.crt -days 3650 -sha256 \
    -extfile "$san_config" -extensions v3_req -CAcreateserial

# Full chain: server cert + CA cert
cat all-services.crt local_ca.pem > all-services-chain.crt

# Create keystore with server cert + key
openssl pkcs12 -export -out all-services.keystore.p12 \
    -in all-services-chain.crt -inkey all-services.key \
    -name localhost -macalg SHA256 -passout "pass:$PASSWORD"

# Import CA into keystore
keytool -importcert -keystore all-services.keystore.p12 -alias "zowe development instances certificate authority" \
    -file local_ca.pem -noprompt -storepass "$PASSWORD" -storetype pkcs12

# server-only.p12 — the all-services certificate without the CA in its own chain,
# used by the Docker services that present a leaf and let the truststore supply the CA.
openssl pkcs12 -export -out server-only.p12 \
    -in all-services.crt -inkey all-services.key \
    -name localhost -macalg SHA256 \
    -passout "pass:$PASSWORD"

# The CA still goes in as a trusted entry, same alias as all-services.keystore.p12
keytool -importcert -keystore server-only.p12 -alias "zowe development instances certificate authority" \
    -file local_ca.pem -noprompt -storepass "$PASSWORD" -storetype pkcs12

# Create truststore
keytool -import -alias "zowe development instances certificate authority" \
    -file local_ca.pem -keystore all-services.truststore.p12 \
    -storetype pkcs12 -storepass "$PASSWORD" -noprompt

finalize_truststore all-services.truststore.p12

# Convenience exports
cp all-services.crt all-services.keystore.cer
cp all-services.key all-services.keystore.key
cp all-services.crt all-services.cer
cat all-services.key > all-services.pem
cat all-services.crt >> all-services.pem

# client-cert.p12 — used by integration tests as a client keystore. Its key entry keeps
# the "CN=zowe component, O=OMP" subject that test assertions match on.
echo ""
echo "--- Generating client-cert keystore ---"

# Generate key and CSR
openssl genrsa -out client-cert.key 2048
cat > client-cert-san.cnf <<EOC
[ req ]
default_bits            = 2048
prompt                  = no
default_md              = sha256
distinguished_name      = dn

[ dn ]
CN = "zowe component"
O  = "OMP"

[ v3_req ]
extendedKeyUsage        = clientAuth
EOC

openssl req -new -key client-cert.key -out client-cert.csr \
    -config client-cert-san.cnf

# Sign with local CA
openssl x509 -req -in client-cert.csr \
    -CA local_ca.pem -CAkey local_ca.key -CAcreateserial \
    -days 3650 -out client-cert.crt \
    -extfile client-cert-san.cnf -extensions v3_req

# Create PKCS12 keystore
openssl pkcs12 -export -out client-cert.p12 \
    -in client-cert.crt -inkey client-cert.key \
    -name localhost -macalg SHA256 -passout "pass:$PASSWORD"

# Import CA certificate as trusted cert entry
keytool -importcert -keystore client-cert.p12 \
    -alias "zowe development instances certificate authority" \
    -file local_ca.pem -noprompt -storepass "$PASSWORD" -storetype pkcs12

# Clean up
rm -f client-cert.key client-cert.csr client-cert.crt client-cert-san.cnf
rm -f all-services.key all-services.csr all-services.crt all-services-chain.crt local_ca.key local_ca.pem "$san_config"

# ── 5. Client certificates ─────────────────────────────────────────────────
echo ""
echo "=== Generating client certificates ==="
cd "$KEYSTORE_DIR/client_cert"

# Generate APIML External CA
generate_ca apiml_ca.key apiml_ca.crt "C = CZ
ST = Czechia
L = Prague
O = OMF
OU = Zowe
CN = APIML CA"

openssl pkcs12 -export -out ca/apiml_ca.p12 \
    -in apiml_ca.crt -inkey apiml_ca.key \
    -name apiml_ca -macalg SHA256 -passout "pass:$PASSWORD"

# Generate client certificates signed by APIML External CA
generate_client_cert() {
    local cn="$1"
    local alias="$2"

    openssl req -newkey rsa:2048 -nodes -keyout "${cn}.key" \
        -out "${cn}.csr" -sha256 \
        -subj "/C=CZ/ST=Czechia/L=Prague/O=OMF/OU=Zowe/CN=$cn"

    openssl x509 -req -in "${cn}.csr" \
        -CA apiml_ca.crt -CAkey apiml_ca.key \
        -out "${cn}.crt" -days 3650 -sha256 -CAcreateserial

    openssl pkcs12 -export -out "${cn}.p12" \
        -in "${cn}.crt" -inkey "${cn}.key" \
        -name "$alias" -macalg SHA256 -passout "pass:$PASSWORD"

    rm -f "${cn}.key" "${cn}.csr" "${cn}.crt"
}

generate_client_cert "APIMTST" "apimtst"
generate_client_cert "USER" "user"
generate_client_cert "UNKNOWNUSER" "unknownuser"

# Combined client-certs.p12
keytool -importkeystore -srckeystore APIMTST.p12 -srcstorepass "$PASSWORD" \
    -destkeystore client-certs.p12 -deststorepass "$PASSWORD" -deststoretype pkcs12 -noprompt
keytool -importkeystore -srckeystore USER.p12 -srcstorepass "$PASSWORD" \
    -destkeystore client-certs.p12 -deststorepass "$PASSWORD" -deststoretype pkcs12 -noprompt
keytool -importkeystore -srckeystore UNKNOWNUSER.p12 -srcstorepass "$PASSWORD" \
    -destkeystore client-certs.p12 -deststorepass "$PASSWORD" -deststoretype pkcs12 -noprompt

# Import APIML CA into the combined keystore
keytool -importcert -keystore client-certs.p12 -alias apiml_ca \
    -file apiml_ca.crt -noprompt -storepass "$PASSWORD" -storetype pkcs12

# Import APIML CA into Docker truststore so mock services trust client certificates
keytool -importcert -keystore "$KEYSTORE_DIR/docker/all-services.truststore.p12" \
    -alias "apiml ca" -file apiml_ca.crt \
    -noprompt -storepass "$PASSWORD" -storetype pkcs12

# Import APIML CA into localhost truststore (used by mock-services for X509 client auth)
keytool -importcert -keystore "$KEYSTORE_DIR/localhost/localhost.truststore.p12" \
    -alias "apiml ca" -file apiml_ca.crt \
    -noprompt -storepass "$PASSWORD" -storetype pkcs12

# Clean up individual p12 files and intermediates
rm -f APIMTST.p12 USER.p12 UNKNOWNUSER.p12 apiml_ca.key apiml_ca.crt apiml_ca.srl

# ── 6. Copy keystores to test resource locations ───────────────────────────
echo ""
echo "=== Copying keystores to test resources ==="
cp "$KEYSTORE_DIR/localhost/localhost.keystore.p12" "$REPO_ROOT/zaas-client/src/test/resources/localhost.keystore.p12"
cp "$KEYSTORE_DIR/localhost/localhost.truststore.p12" "$REPO_ROOT/zaas-client/src/test/resources/localhost.truststore.p12"

# Extract JWT public key for SecurityUtilsTest (common-service-core test fixture)
echo "Extracting JWT public key for test fixtures..."
openssl pkcs12 -in "$KEYSTORE_DIR/localhost/localhost.keystore.p12" \
    -passin pass:"$PASSWORD" -nokeys 2>/dev/null \
    | openssl x509 -pubkey -noout 2>/dev/null \
    | openssl pkey -pubin -outform DER 2>/dev/null \
    | openssl base64 -A > "$REPO_ROOT/common-service-core/src/test/resources/jwt-public-key.pub"

# ── 7. Sanity checks ───────────────────────────────────────────────────────
# Assert the properties tests depend on, so a change to the generation logic fails
# here instead of surfacing as an unrelated TLS test failure much later.
echo ""
echo "=== Verifying generated keystores ==="

assert_san_absent() {
    local ks="$1" host="$2"
    if keytool -list -v -keystore "$ks" -storepass "$PASSWORD" 2>/dev/null \
        | grep -q "DNSName: $host"; then
        echo "ERROR: $ks must NOT carry a SAN entry for '$host'" >&2
        exit 1
    fi
}

assert_san_present() {
    local ks="$1" host="$2"
    if ! keytool -list -v -keystore "$ks" -storepass "$PASSWORD" 2>/dev/null \
        | grep -q "DNSName: $host"; then
        echo "ERROR: $ks is missing a SAN entry for '$host'" >&2
        exit 1
    fi
}

assert_subject_cn() {
    local ks="$1" cn="$2"
    if ! keytool -list -v -keystore "$ks" -storepass "$PASSWORD" 2>/dev/null \
        | grep -q "^Owner: CN=$cn,"; then
        echo "ERROR: $ks is missing a certificate with common name '$cn'" >&2
        exit 1
    fi
}

assert_issuer_cn() {
    local ks="$1" cn="$2"
    if ! keytool -list -v -keystore "$ks" -storepass "$PASSWORD" 2>/dev/null \
        | grep -q "^Issuer: CN=$cn,"; then
        echo "ERROR: $ks must be signed by '$cn'" >&2
        exit 1
    fi
}

# Must not match localhost: backs the strict hostname verification tests
assert_san_absent  "$KEYSTORE_DIR/localhost/nonlocalhost.keystore.p12"      localhost
# Must match: normal service certificates
assert_san_present "$KEYSTORE_DIR/localhost/localhost.keystore.p12"         localhost
assert_san_present "$KEYSTORE_DIR/selfsigned/localhost.keystore.p12"        localhost
assert_san_present "$KEYSTORE_DIR/docker/all-services.keystore.p12"         gateway-service
assert_san_present "$KEYSTORE_DIR/docker/all-services.keystore.p12"         discovery-service
# config/local-multi reaches the second instance as https://localhost2:10021
assert_san_present "$KEYSTORE_DIR/localhost/localhost-multi.keystore.p12"   localhost2
assert_san_present "$KEYSTORE_DIR/localhost/localhost-multi.keystore.p12"   localhost3
# apiml.security.x509.registry.allowedUsers in config/local matches on this common name
assert_subject_cn  "$KEYSTORE_DIR/localhost/localhost.keystore.p12"         "Zowe Service"
# localhost2 belongs to the second CA — both TomcatHttpsTest cases that use it rely on it
# being foreign to the main PKI
assert_issuer_cn   "$KEYSTORE_DIR/localhost/localhost2.keystore.p12" \
    "Zowe Development Instances Certificate Authority 2"

echo "All checks passed."

# ── Done ───────────────────────────────────────────────────────────────────
# Fix permissions: OpenSSL creates .key files with 0600, which the OpenTelemetry
# collector container (Register job) runs as a non-root user and cannot read.
# Scope this to the one key that is actually mounted rather than every private key.
chmod 644 "$KEYSTORE_DIR/localhost/localhost.keystore.key"

# Stamp the tree with the checksum of the script that produced it.
if command -v sha256sum &>/dev/null; then
    sha256sum "$SCRIPT_PATH" | cut -d' ' -f1 > "$STAMP_FILE"
else
    shasum -a 256 "$SCRIPT_PATH" | cut -d' ' -f1 > "$STAMP_FILE"
fi

echo ""
echo "=== Keystore generation complete ==="
echo "All keystores generated under: $KEYSTORE_DIR"
echo "Test resources copied to: zaas-client/src/test/resources/"
echo ""
echo "Generated keystores:"
find "$KEYSTORE_DIR" -type f \( -name '*.p12' -o -name '*.cer' -o -name '*.key' -o -name '*.pem' \) | sort | while read -r f; do
    echo "  ${f#"$REPO_ROOT"/}"
done
