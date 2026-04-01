# z/OSMF JWT Check Tool

A Java utility that verifies connectivity to the z/OSMF JWK endpoint. This tool helps diagnose configuration issues early such as incorrect hostnames, unreachable ports, missing certificates, or misconfigured z/OSMF by performing a lightweight HTTP(S) call to the z/OSMF JWK endpoint at `/jwt/ibm/api/zOSMFBuilder/jwk`.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building](#building)
- [Usage](#usage)
- [CLI Flags Reference](#cli-flags-reference)
- [Certificate Verification Modes](#certificate-verification-modes)
- [Exit Codes](#exit-codes)
- [Response Interpretation](#response-interpretation)
- [Testing Scenarios](#testing-scenarios)
  - [1. Quick Test — DISABLED Mode](#1-quick-test--disabled-mode-no-truststore-needed)
  - [2. STRICT Mode — Full Verification](#2-strict-mode--full-certificate-and-hostname-verification)
  - [3. NONSTRICT Mode — Skip Hostname Check](#3-nonstrict-mode--certificate-chain-verified-hostname-check-skipped)
  - [4. HTTP Mode (No SSL)](#4-http-mode-no-ssl)
  - [5. Validation Error Tests](#5-validation-error-tests)
- [SAF Keyrings](#saf-keyrings)
- [Troubleshooting](#troubleshooting)

---
## Prerequisites

- **Java 17 or higher** (Java 17, 21, or any later version)
- Network access to the z/OSMF server
- A truststore containing the z/OSMF server's CA certificate (required for STRICT and NONSTRICT modes)

## Building

From the root of the `api-layer` repository:

```bash
./gradlew :zosmf-jwt-check:build
```

On Windows:

```powershell
.\gradlew :zosmf-jwt-check:build
```

The fat JAR (with all dependencies bundled) will be generated at:

```
zosmf-jwt-check/build/libs/zosmf-jwt-check-<version>.jar
```

For example: `zosmf-jwt-check/build/libs/zosmf-jwt-check-3.5.12-SNAPSHOT.jar`

## Usage

```bash
java -jar zosmf-jwt-check-<version>.jar --zosmf-host <hostname> --zosmf-port <port> [options]
```

**Minimal example (DISABLED mode,  quickest way to test):**

```bash
java -jar zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --verify-certificates DISABLED
```

**Full example (STRICT mode with truststore):**

```bash
java -jar zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore /path/to/truststore.p12 \
  --truststore-password changeit
```

**Display help:**

```bash
java -jar zosmf-jwt-check-<version>.jar --help
```

## CLI Flags Reference

### Required Flags

| Flag | Description | Example |
|------|-------------|---------|
| `--zosmf-host` | Hostname or IP address of the z/OSMF server | `--zosmf-host myzosmf.example.com` |
| `--zosmf-port` | Port number of the z/OSMF server | `--zosmf-port 11443` |

> **Note:** If `--zosmf-host` or `--zosmf-port` are omitted, picocli will display:
> `Missing required option: '--zosmf-host=<zosmfHost>'`

### Conditionally Required Flags

These flags are required when `--scheme=https` (the default) and `--verify-certificates` is **not** `DISABLED`:

| Flag | Description | Error when missing |
|------|-------------|-------------------|
| `--truststore` | Path to the truststore file containing the z/OSMF CA certificate | `ERROR: --truststore is required when --scheme=https and verification is not DISABLED.` |
| `--truststore-password` | Password for the truststore. If specified without a value, you will be prompted interactively. | `ERROR: --truststore-password is required when --scheme=https and verification is not DISABLED.` |

### Optional Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--scheme` | `https` | Protocol to use: `http` or `https` |
| `--verify-certificates` | `STRICT` | Certificate verification mode: `STRICT`, `NONSTRICT`, or `DISABLED` |
| `--truststore-type` | `PKCS12` | Format of the truststore file (e.g., `PKCS12`, `JKS`, `JCERACFKS`) |
| `--keystore` | *(none)* | Path to keystore file (only needed for mutual TLS / client certificate authentication) |
| `--keystore-password` | *(none)* | Password for the keystore. If specified without a value, you will be prompted interactively. |
| `--keystore-type` | `PKCS12` | Format of the keystore file |
| `-h`, `--help` | | Display usage help and exit |

## Certificate Verification Modes

The `--verify-certificates` flag controls how SSL/TLS certificates are validated when connecting over HTTPS. This mirrors the `zowe.verifyCertificates` setting in the Zowe configuration (`zowe.yaml`).

### STRICT (Default)

```bash
--verify-certificates STRICT
```

- **Certificate chain**: Fully validated against the truststore
- **Hostname verification**: The server certificate's CN/SAN must match the `--zosmf-host` value
- **Truststore**: Required
- **Use case**: Production environments — maximum security

### NONSTRICT

```bash
--verify-certificates NONSTRICT
```

- **Certificate chain**: Fully validated against the truststore
- **Hostname verification**: Skipped — the server certificate does not need to match the hostname
- **Truststore**: Required
- **Use case**: Environments where the z/OSMF certificate is issued for a different hostname (e.g., accessing via IP address when the cert has a DNS name)

### DISABLED

```bash
--verify-certificates DISABLED
```

- **Certificate chain**: Not validated — all certificates are trusted
- **Hostname verification**: Skipped
- **Truststore**: Not required
- **Use case**: Development/testing environments, or initial connectivity debugging
- **Warning**: Prints `WARNING: SSL certificate verification is DISABLED. All certificates will be trusted.`

> **Security Note:** `DISABLED` mode should **never** be used in production. It is vulnerable to man-in-the-middle attacks.

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | **Success** — z/OSMF JWK endpoint is reachable and responding |
| `4` | **Failure** — connection failed, SSL error, endpoint not found, or configuration error |
| `8` | **Help** — help/version was displayed; no check was performed |

## Response Interpretation

The tool interprets HTTP response codes from the z/OSMF JWK endpoint as follows:

| HTTP Code | Result | Message |
|-----------|--------|---------|
| 200-299 | **SUCCESS** | `z/OSMF JWK endpoint is reachable and responding. HTTP <code>` |
| 401 | **SUCCESS** | `z/OSMF JWK endpoint exists (returned 401 Unauthorized — expected without credentials). HTTP 401` |
| 404 | **FAILURE** | `z/OSMF JWK endpoint not found. HTTP 404` — Consider configuring `jwtAutoConfiguration` to LTPA |
| 4xx (other) | **FAILURE** | `z/OSMF JWK endpoint returned unexpected client error. HTTP <code>` |
| 5xx | **FAILURE** | `z/OSMF JWK endpoint returned server error. HTTP <code>` |

**Note:** A `401 Unauthorized` is treated as **success** because the tool does not send authentication credentials. A 401 confirms the endpoint exists and z/OSMF is processing requests.

### Connection-Level Errors

| Error | Message |
|-------|---------|
| SSL handshake failure | `FAILURE: SSL handshake failed. Verify that the truststore contains the z/OSMF server certificate.` |
| Connection refused | `FAILURE: Cannot connect to <host>:<port>. Verify the host and port are correct and z/OSMF is running.` |
| Connection timeout | `FAILURE: Connection timed out to <host>:<port>.` |

## Testing Scenarios

Below are step-by-step commands for testing all modes. Replace `<version>` with your actual JAR version (e.g., `3.5.12-SNAPSHOT`) and adjust the host/port for your environment.

### 1. Quick Test — DISABLED Mode (No Truststore Needed)

The fastest way to verify basic TCP + HTTP connectivity:

```bash
java -jar zosmf-jwt-check/build/libs/zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --verify-certificates DISABLED
```

**Expected output (success):**

```
WARNING: SSL certificate verification is DISABLED. All certificates will be trusted.
Checking z/OSMF JWK endpoint: https://myzosmf.example.com:11443/jwt/ibm/api/zOSMFBuilder/jwk
SUCCESS: z/OSMF JWK endpoint exists (returned 401 Unauthorized — expected without credentials). HTTP 401
```

### 2. STRICT Mode — Full Certificate and Hostname Verification

Requires a truststore containing the z/OSMF server's CA certificate (see [Creating a Truststore](#creating-a-truststore)):

```bash
java -jar zosmf-jwt-check/build/libs/zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore /path/to/zosmf-truststore.p12 \
  --truststore-password password
```

**Expected output (success):**

```
Checking z/OSMF JWK endpoint: https://myzosmf.example.com:11443/jwt/ibm/api/zOSMFBuilder/jwk
SUCCESS: z/OSMF JWK endpoint exists (returned 401 Unauthorized — expected without credentials). HTTP 401
```

**Expected output (SSL failure — wrong truststore):**

```
FAILURE: SSL handshake failed when connecting to https://myzosmf.example.com:11443/jwt/ibm/api/zOSMFBuilder/jwk.
Verify that the truststore contains the z/OSMF server certificate.
Details: PKIX path building failed: ...unable to find valid certification path to requested target
```

### 3. NONSTRICT Mode — Certificate Chain Verified, Hostname Check Skipped

Useful when connecting via IP address but the certificate has a DNS name:

```bash
java -jar zosmf-jwt-check/build/libs/zosmf-jwt-check-<version>.jar \
  --zosmf-host 10.0.0.50 \
  --zosmf-port 11443 \
  --truststore /path/to/zosmf-truststore.p12 \
  --truststore-password password \
  --verify-certificates NONSTRICT
```

**Expected output (success):**

```
INFO: Hostname verification is disabled (NONSTRICT mode).
Checking z/OSMF JWK endpoint: https://10.0.0.50:11443/jwt/ibm/api/zOSMFBuilder/jwk
SUCCESS: z/OSMF JWK endpoint exists (returned 401 Unauthorized — expected without credentials). HTTP 401
```

### 4. HTTP Mode (No SSL)

For z/OSMF instances running on plain HTTP (uncommon):

```bash
java -jar zosmf-jwt-check/build/libs/zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 80 \
  --scheme http
```

### 5. Validation Error Tests

**Missing required flags:**

```bash
# No arguments at all
java -jar zosmf-jwt-check-<version>.jar
# Output: Missing required options: '--zosmf-host=<zosmfHost>', '--zosmf-port=<zosmfPort>'

# Missing truststore in STRICT mode
java -jar zosmf-jwt-check-<version>.jar --zosmf-host myhost --zosmf-port 443
# Output: ERROR: --truststore is required when --scheme=https and verification is not DISABLED.

# Missing truststore password
java -jar zosmf-jwt-check-<version>.jar --zosmf-host myhost --zosmf-port 443 --truststore my.p12
# Output: ERROR: --truststore-password is required when --scheme=https and verification is not DISABLED.
```

**Invalid values:**

```bash
# Invalid scheme
java -jar zosmf-jwt-check-<version>.jar --zosmf-host myhost --zosmf-port 443 --scheme ftp
# Output: ERROR: --scheme must be 'http' or 'https', got: ftp

# Invalid verify mode
java -jar zosmf-jwt-check-<version>.jar --zosmf-host myhost --zosmf-port 443 --verify-certificates INVALID
# Output: ERROR: --verify-certificates must be STRICT, NONSTRICT, or DISABLED, got: INVALID
```

**Unreachable host:**

```bash
java -jar zosmf-jwt-check-<version>.jar --zosmf-host nonexistent.host --zosmf-port 443 --verify-certificates DISABLED
# Output: FAILURE: Cannot connect to nonexistent.host:443.
```

## SAF Keyrings

On z/OS, if you are using SAF keyrings instead of file-based keystores/truststores, provide the keyring path in the `safkeyring://` format and add the JVM protocol handler:

```bash
java -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
  -jar zosmf-jwt-check-<version>.jar \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore safkeyring://IZUSVR/ZoweKeyring \
  --truststore-password password \
  --truststore-type JCERACFKS
```
