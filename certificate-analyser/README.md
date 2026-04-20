## Certificate validation tool

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
                Path to keystore file or keyring. When using keyring, pass
                                  -Djava.protocol.handler.pkgs=com.ibm.crypto.provider in
                                  command line.
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

### Keyring

If you are using SAF keyrings, you need to provide an additional parameter in command line `-Djava.protocol.handler.pkgs=com.ibm.crypto.provider`.

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
java -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
  -jar certificate-analyser-<version>.jar --zosmf-jwt-check \
  --zosmf-host myzosmf.example.com \
  --zosmf-port 11443 \
  --truststore-file safkeyring://IZUSVR/ZoweKeyring \
  --truststore-password password \
  --truststore-type JCERACFKS
```
