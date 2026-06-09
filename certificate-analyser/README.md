## Certificate validation tool

### Build

From the root of the `api-layer` repository:

```bash
./gradlew :certificate-analyser:build
```

On Windows:

```powershell
.\gradlew :certificate-analyser:build
```

The fat JAR (with all dependencies bundled, including z/OSMF JWT Check) will be generated at:

```
certificate-analyser/build/libs/certificate-analyser-<version>.jar
```

#### Build Architecture

The `certificate-analyser` module produces a **single unified fat JAR** that bundles:

1. All certificate-analyser classes (certificate validation, local/remote handshake, local verification)
2. All `zosmf-jwt-check` classes (z/OSMF JWK endpoint connectivity check)
3. All shared dependencies (picocli)

This is achieved through the following Gradle configuration:

- `certificate-analyser/build.gradle` declares `implementation project(':zosmf-jwt-check')` as a dependency
- The `jar` task uses `from { configurations.runtimeClasspath.collect { ... } }` to bundle all runtime dependencies into the fat JAR
- `zosmf-jwt-check/build.gradle` produces a thin JAR (no `Main-Class` manifest, no fat-jar bundling) that is consumed only as a build dependency
- The `zosmf-jwt-check` module **does not produce a standalone runnable JAR** — its functionality is accessed exclusively through the certificate-analyser JAR via the `--zosmf-jwt-check` CLI flag

### Usage

java -jar certificate-analyser-<version>.jar --help

```
Usage: <main class> [-hl] [-kp[=<keyPasswd>]] [-tp[=<trustPasswd>]]
                    [-a=<keyAlias>] [-k=<keyStore>] [-kt=<keyStoreType>]
                    [-r=<remoteUrl>] [-t=<trustStore>] [-tt=<trustStoreType>]
  -a, --keyalias=<keyAlias>
                Alias under which this key is stored
  -h, --help    display a help message
  -k, --keystore=<keyStore>
                Path to keystore file or keyring (safkeyring://userId/keyRing).
      -kp, --keypasswd[=<keyPasswd>]
                Keystore password
      -kt, --keystoretype=<keyStoreType>
                Keystore type, default is PKCS12
  -l, --local   Do SSL handshake on localhost
  -r, --remoteurl=<remoteUrl>
                URL of service to be verified
  -t, --truststore=<trustStore>
                Path to truststore file or keyring
      -tp, --trustpasswd[=<trustPasswd>]
                Truststore password
      -tt, --truststoretype=<trustStoreType>
                Truststore type, default is PKCS12
```

*NOTE*

keypasswd - if you specify this parameter without a value(e.g. java -jar <file.jar> --keypasswd), you will be asked to enter the password

trustpasswd - if you specify this parameter without a value(e.g. java -jar <file.jar> --trustpasswd), you will be asked to enter the password
            -  if this parameter is omitted completely, value from keypasswd will be used

truststoretype - if this parameter is omitted completely, value from keystoretype will be used

### Do local handshake

java -jar -Djavax.net.debug=ssl:handshake:verbose certificate-analyser-<version>.jar --keystore ../../../keystore/localhost/localhost.keystore.p12 --truststore ../../../keystore/localhost/localhost.truststore.p12 --keypasswd password --keyalias localhost --local

### SAF Keyrings

On z/OS with IBM Java 17/21, if you are using SAF keyrings, add the IBM crypto modules to the JVM module graph:

```bash
java --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca \
  -jar certificate-analyser-<version>.jar \
  --keystore safkeyring://userId/keyRing \
  --keystoretype JCERACFKS \
  --keypasswd password
```

> **Note:** The tool internally sets `java.protocol.handler.pkgs` to register the
> `safkeyring://` URL protocol handler. The `--add-modules` flag resolves the IBM
> crypto modules making the handler classes accessible. Both mechanisms work together
> to enable `safkeyring://` URLs — no additional `-D` flags are needed.

### Possible issues

Keystore/truststore is owned by different user - permission error. Temporarily Change read permission to all.

---

## z/OSMF JWT Check (embedded)

This JAR also includes the **z/OSMF JWT Check** tool, which verifies connectivity to the z/OSMF JWK endpoint (`/jwt/ibm/api/zOSMFBuilder/jwk`). It helps diagnose configuration issues such as incorrect hostnames, unreachable ports, missing certificates, or misconfigured z/OSMF.

To use the z/OSMF JWT check functionality, pass `--zosmf-jwt-check` as the **first** argument. All subsequent arguments are passed to the z/OSMF JWT check tool.

### z/OSMF JWT Check — Help

```bash
java -jar certificate-analyser-<version>.jar --zosmf-jwt-check --help
```

### z/OSMF JWT Check — CLI Flags

#### Required Flags

| Flag | Description | Example |
|------|-------------|---------|
| `--zosmf-host` | Hostname or IP address of the z/OSMF server | `--zosmf-host myzosmf.example.com` |
| `--zosmf-port` | Port number of the z/OSMF server | `--zosmf-port 11443` |

#### Conditionally Required Flags

These flags are required when `--scheme=https` (the default) and `--verify-certificates` is **not** `DISABLED`:

| Flag | Description |
|------|-------------|
| `--truststore-file` | Path to the truststore file containing the z/OSMF CA certificate |
| `--truststore-password` | Password for the truststore. If specified without a value, you will be prompted interactively |

#### Optional Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--scheme` | `https` | Protocol to use: `http` or `https` |
| `--verify-certificates` | `STRICT` | Certificate verification mode: `STRICT`, `NONSTRICT`, or `DISABLED` |
| `--truststore-type` | `PKCS12` | Format of the truststore file (e.g., `PKCS12`, `JKS`, `JCERACFKS`) |
| `--keystore-file` | *(none)* | Path to keystore file (only needed for mutual TLS / client certificate authentication) |
| `--keystore-password` | *(none)* | Password for the keystore. If specified without a value, you will be prompted interactively |
| `--keystore-type` | `PKCS12` | Format of the keystore file |
| `-v`, `--verbose` | | Print the response body from the endpoint |
| `-h`, `--help` | | Display usage help and exit |

### Certificate Verification Modes

- **STRICT** (default) — Full certificate chain validation + hostname verification. Truststore required.
- **NONSTRICT** — Certificate chain validated, hostname verification skipped. Truststore required.
- **DISABLED** — No certificate validation. No truststore required. **Do not use in production.**

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | **Success** — z/OSMF JWK endpoint is reachable and responding |
| `4` | **Failure** — connection failed, SSL error, endpoint not found, or configuration error |
| `8` | **Help** — help/version was displayed; no check was performed |

### z/OSMF JWT Check — Examples

**Quick test (DISABLED mode — no truststore needed):**

```bash
java -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --verify-certificates DISABLED
```

**STRICT mode (full certificate verification):**

```bash
java -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore-file /path/to/truststore.p12 \
  --truststore-password changeit
```

**NONSTRICT mode (skip hostname check):**

```bash
java -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host 10.0.0.50 \
  --zosmf-port 11443 \
  --truststore-file /path/to/truststore.p12 \
  --truststore-password password \
  --verify-certificates NONSTRICT
```

**HTTP mode (no SSL):**

```bash
java -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 80 \
  --scheme http
```

### SAF Keyrings (z/OSMF JWT Check)

On z/OS, if you are using SAF keyrings, provide the keyring path in `safkeyring://` format and add the JVM protocol handler:

```bash
java --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca \
  -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore-file safkeyring://IZUSVR/ZoweKeyring \
  --truststore-password password \
  --truststore-type JCERACFKS
```

> **Note:** On IBM Java 17/21, the `--add-modules` flag resolves the IBM crypto modules,
> making the safkeyring URL protocol handler classes accessible. The tool also sets
> `java.protocol.handler.pkgs` internally to register the handler packages with
> the `URL` class. Both mechanisms work together to enable `safkeyring://` URLs.
