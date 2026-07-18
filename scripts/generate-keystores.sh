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
    for sdk_java in "$HOME/.sdkman/candidates/java/"{17.*,21.*,current,current/bin}; do
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

# Use the keytool directory for the Java bin path (needed for openssl to find keystore provider sometimes)
JAVA_BIN="$(dirname "$KEYTOOL")"
export PATH="$JAVA_BIN:$PATH"

if ! command -v openssl &>/dev/null; then
    echo "ERROR: openssl not found. Please install openssl." >&2
    exit 1
fi

echo "Using keytool: $KEYTOOL ($(keytool -version 2>&1 || true))"
echo "Using openssl: $(openssl version)"

# Find Java cacerts for importing public CA certificates
JAVA_CACERTS="${JAVA_HOME:+$JAVA_HOME/lib/security/cacerts}"
if [ ! -f "$JAVA_CACERTS" ]; then
    JAVA_CACERTS=$(find / -name "cacerts" -path "*/security/*" -not -path "/mnt/*" 2>/dev/null | head -1)
fi
if [ -n "$JAVA_CACERTS" ]; then
    echo "Found Java cacerts: $JAVA_CACERTS"
else
    echo "WARNING: Could not find Java cacerts truststore. External TLS connections may fail."
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYSTORE_DIR="$REPO_ROOT/keystore"

# ── Passwords ──────────────────────────────────────────────────────────────
PASSWORD="password"
CA_PASSWORD="local_ca_password"

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

# Generate CA private key
openssl genrsa -out local_ca.key 2048

# Generate self-signed CA certificate
openssl req -x509 -new -nodes -key local_ca.key -sha256 -days 3650 \
    -out local_ca.pem \
    -subj "/C=CZ/ST=Prague/L=Prague/O=Broadcom/OU=MFD/CN=Zowe Development Instances Certificate Authority"

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

# Generate a second CA for truststore mismatch tests (localhost2)
# This CA is intentionally different from the main CA so that localhost2.truststore
# does NOT trust certificates signed by the main local CA
echo ""
echo "=== Generating Secondary Certificate Authority (for truststore mismatch tests) ==="
openssl genrsa -out local_ca2.key 2048
openssl req -x509 -new -nodes -key local_ca2.key -sha256 -days 3650 \
    -out local_ca2.pem \
    -subj "/C=CZ/ST=Prague/L=Prague/O=Broadcom/OU=MFD/CN=Zowe Secondary Development CA"
openssl x509 -in local_ca2.pem -outform DER -out localca2.cer
rm -f local_ca2.key local_ca2.pem

# ── 2. Localhost keystores ─────────────────────────────────────────────────
echo ""
echo "=== Generating localhost keystores ==="
cd "$KEYSTORE_DIR/localhost"

generate_localhost_keystore() {
    local cn="$1"          # Common Name for the certificate
    local alias="$2"       # Alias in the keystore
    local ks_name="$3"     # Output keystore filename
    local ts_name="$4"     # Output truststore filename
    local ca_cert="$5"     # CA certificate to trust
    local ca_key="$6"      # CA private key file
    local ca_pass="$7"     # CA keystore password
    local extra_san="${8:-}" # Optional extra SAN entries

    # Generate server key and CSR
    local san_config
    if [ -n "$extra_san" ]; then
        san_config=$(mktemp)
        cat > "$san_config" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C = CZ
ST = Czechia
L = Prague
O = Broadcom Inc
OU = IT
CN = $cn

[v3_req]
keyUsage = digitalSignature, nonRepudiation, keyEncipherment
extendedKeyUsage = clientAuth, serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = 127.0.0.1
$extra_san
EOF
    else
        san_config=$(mktemp)
        cat > "$san_config" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C = CZ
ST = Czechia
L = Prague
O = Broadcom Inc
OU = IT
CN = $cn

[v3_req]
keyUsage = digitalSignature, nonRepudiation, keyEncipherment
extendedKeyUsage = clientAuth, serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = 127.0.0.1
EOF
    fi

    # Generate private key
    openssl genrsa -out "${cn}.key" 2048

    # Generate CSR
    openssl req -new -key "${cn}.key" -out "${cn}.csr" -config "$san_config"

    # Sign with CA
    openssl x509 -req -in "${cn}.csr" \
        -CA "$ca_cert" -CAkey "$ca_key" \
        -out "${cn}.crt" -days 3650 -sha256 \
        -extfile "$san_config" -extensions v3_req -CAcreateserial

    # Create truststore with CA cert
    keytool -import -alias localca -file "$ca_cert" \
        -keystore "$ts_name" -storetype pkcs12 -storepass "$PASSWORD" -noprompt

    # Create PKCS12 keystore with server cert + key
    openssl pkcs12 -export -out "$ks_name" \
        -in "${cn}.crt" -inkey "${cn}.key" \
        -name "$alias" -macalg SHA256 -passout "pass:$PASSWORD"

    # Import CA into keystore
    keytool -importcert -keystore "$ks_name" -alias localca \
        -file "$ca_cert" -noprompt -storepass "$PASSWORD" -storetype pkcs12

    # Clean up
    rm -f "${cn}.key" "${cn}.csr" "${cn}.crt" "$san_config"
}

# --- localhost standard ---
generate_localhost_keystore \
    "localhost" "localhost" \
    "localhost.keystore.p12" "localhost.truststore.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" "$CA_PASSWORD"

# Export convenience files
keytool -exportcert -keystore localhost.keystore.p12 -alias localhost \
    -storepass "$PASSWORD" -storetype pkcs12 -rfc -file localhost.pem
openssl pkcs12 -in localhost.keystore.p12 -nocerts -nodes \
    -passin "pass:$PASSWORD" -out localhost.keystore.key
keytool -exportcert -keystore localhost.keystore.p12 -alias localhost \
    -storepass "$PASSWORD" -storetype pkcs12 -file localhost.keystore.cer 2>/dev/null || true

# --- localhost2 ---
generate_localhost_keystore \
    "localhost2" "localhost" \
    "localhost2.keystore.p12" "localhost2.truststore.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" "$CA_PASSWORD"

# Overwrite localhost2 truststore with secondary CA (intentionally different from main CA)
# so that trustStoreWithDifferentCertificateAuthorityShouldFail test works
rm -f localhost2.truststore.p12
keytool -import -alias localca -file "$KEYSTORE_DIR/local_ca/localca2.cer" \
    -keystore localhost2.truststore.p12 -storetype pkcs12 -storepass "$PASSWORD" -noprompt

# --- nonlocalhost ---
generate_localhost_keystore \
    "nonlocalhost" "nonlocalhost" \
    "nonlocalhost.keystore.p12" "does-not-matter.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" "$CA_PASSWORD"
rm -f does-not-matter.p12

# --- localhost-multi (uses different alias and CA alias) ---
generate_localhost_keystore \
    "localhost-multi" "localhost-multi" \
    "localhost-multi.keystore.p12" "localhost-multi.truststore.p12" \
    "../local_ca/localca.pem" "../local_ca/local_ca.key" "$CA_PASSWORD"

# Trusted CAs chain for NGINX proxy
cat ../local_ca/localca.cer > trusted_CAs.cer

# Import public CA certificates into all truststores
# This is needed for outbound TLS connections to external services (e.g. OIDC providers)
import_cacerts() {
    local truststore="$1"
    if [ -n "$JAVA_CACERTS" ] && [ -f "$JAVA_CACERTS" ]; then
        keytool -importkeystore \
            -srckeystore "$JAVA_CACERTS" -srcstoretype JKS -srcstorepass changeit \
            -destkeystore "$truststore" -deststoretype PKCS12 -deststorepass "$PASSWORD" \
            -noprompt 2>/dev/null || true
    fi
}
for ts in localhost.truststore.p12 localhost2.truststore.p12 localhost-multi.truststore.p12; do
    if [ -f "$ts" ]; then
        import_cacerts "$ts"
    fi
done

# ── 3. Self-signed keystores ───────────────────────────────────────────────
echo ""
echo "=== Generating self-signed keystores ==="
cd "$KEYSTORE_DIR/selfsigned"

generate_selfsigned_keystore() {
    local cn="$1"
    local alias="$2"
    local ks_name="$3"
    local ts_name="$4"

    # Generate self-signed cert (no CA)
    openssl req -x509 -newkey rsa:2048 -nodes -keyout "${cn}.key" \
        -out "${cn}.crt" -days 3650 -sha256 \
        -subj "/C=CZ/ST=Czechia/L=Prague/O=Broadcom Inc/OU=IT/CN=$cn" \
        -addext "subjectAltName=DNS:localhost,DNS:127.0.0.1" \
        -addext "keyUsage=digitalSignature,nonRepudiation,keyEncipherment" \
        -addext "extendedKeyUsage=clientAuth,serverAuth"

    # Create keystore
    openssl pkcs12 -export -out "$ks_name" \
        -in "${cn}.crt" -inkey "${cn}.key" \
        -name "$alias" -macalg SHA256 -passout "pass:$PASSWORD"

    # Create truststore with self-signed cert
    keytool -import -alias localca -file "${cn}.crt" \
        -keystore "$ts_name" -storetype pkcs12 -storepass "$PASSWORD" -noprompt

    rm -f "${cn}.key" "${cn}.crt"
}

# Standard self-signed
generate_selfsigned_keystore "localhost" "localhost" \
    "localhost.keystore.p12" "localhost.truststore.p12"

# Untrusted self-signed (different CA, not trusted by any API ML service)
generate_selfsigned_keystore "localhost-untrusted" "localhost" \
    "localhost-untrusted.keystore.p12" "does-not-matter.p12"
rm -f does-not-matter.p12

# Create an untrusted truststore from a different self-signed CA
openssl req -x509 -newkey rsa:2048 -nodes -keyout untrusted_ca.key \
    -out untrusted_ca.crt -days 3650 -sha256 \
    -subj "/C=CZ/ST=Czechia/L=Prague/O=Untrusted/OU=IT/CN=Untrusted CA"
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

# Create truststore
keytool -import -alias "zowe development instances certificate authority" \
    -file local_ca.pem -keystore all-services.truststore.p12 \
    -storetype pkcs12 -storepass "$PASSWORD" -noprompt

# Import public CA certificates from the JDK's cacerts truststore
# This is needed for outbound TLS connections to external services (e.g. OIDC providers)
if [ -n "$JAVA_CACERTS" ] && [ -f "$JAVA_CACERTS" ]; then
    echo "Importing public CA certificates from $JAVA_CACERTS"
    keytool -importkeystore \
        -srckeystore "$JAVA_CACERTS" -srcstoretype JKS -srcstorepass changeit \
        -destkeystore all-services.truststore.p12 -deststoretype PKCS12 -deststorepass "$PASSWORD" \
        -noprompt
else
    echo "WARNING: Could not find Java cacerts truststore. External TLS connections may fail."
fi

# Convenience exports
cp all-services.crt all-services.keystore.cer
cp all-services.key all-services.keystore.key
cp all-services.crt all-services.cer
cat all-services.key > all-services.pem
cat all-services.crt >> all-services.pem

# server-only.p12 — created after client-cert.p12 (see below)

# client-cert.p12 — used by integration tests as the server keystore
# Must contain a PrivateKeyEntry with the original subject DN matching test assertions
echo ""
echo "--- Generating client-cert keystore ---"
CLIENT_CERT_CN="zowe component"
CLIENT_CERT_O="OMP"

# Generate key and CSR
openssl genrsa -out client-cert.key 2048
cat > client-cert-san.cnf <<EOC
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = CZ
ST = Czechia
L = Prague
O = ${CLIENT_CERT_O}
CN = ${CLIENT_CERT_CN}

[v3_req]
subjectAltName = @alt_names

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

# server-only.p12 — same as client-cert.p12 (used by mock-services)
cp client-cert.p12 server-only.p12

# Clean up
rm -f client-cert.key client-cert.csr client-cert.crt client-cert-san.cnf

# Clean up
rm -f all-services.key all-services.csr all-services.crt all-services-chain.crt local_ca.key local_ca.pem "$san_config"

# ── 5. Client certificates ─────────────────────────────────────────────────
echo ""
echo "=== Generating client certificates ==="
cd "$KEYSTORE_DIR/client_cert"

# Generate APIML External CA
openssl req -x509 -newkey rsa:2048 -nodes -keyout apiml_ca.key \
    -out apiml_ca.crt -days 3650 -sha256 \
    -subj "/C=CZ/ST=Prague/L=Prague/O=Broadcom/OU=MFD/CN=APIML External Certificate Authority"

openssl pkcs12 -export -out ca/apiml_ca.p12 \
    -in apiml_ca.crt -inkey apiml_ca.key \
    -name apiml_ca -macalg SHA256 -passout "pass:$PASSWORD"

# Generate client certificates signed by APIML External CA
generate_client_cert() {
    local cn="$1"
    local alias="$2"

    openssl req -newkey rsa:2048 -nodes -keyout "${cn}.key" \
        -out "${cn}.csr" -sha256 \
        -subj "/C=CZ/ST=Czechia/L=Prague/O=Broadcom Inc/OU=IT/CN=$cn"

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
    | base64 -w0 > "$REPO_ROOT/common-service-core/src/test/resources/jwt-public-key.pub"

# ── Done ───────────────────────────────────────────────────────────────────
# Fix permissions: OpenSSL creates .key files with 0600 which Docker containers
# (like the OpenTelemetry collector used in the Register job) cannot read
find "$KEYSTORE_DIR" -type f -name '*.key' -exec chmod 644 {} +

echo ""
echo "=== Keystore generation complete ==="
echo "All keystores generated under: $KEYSTORE_DIR"
echo "Test resources copied to: zaas-client/src/test/resources/"
echo ""
echo "Generated keystores:"
find "$KEYSTORE_DIR" -type f \( -name '*.p12' -o -name '*.cer' -o -name '*.key' -o -name '*.pem' \) | sort | while read -r f; do
    echo "  $(realpath --relative-to="$REPO_ROOT" "$f")"
done
