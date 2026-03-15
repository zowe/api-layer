# #4473 — Stored certificates and keys in the repository

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4473
**Labels:** enhancement, Priority: Medium | **Created:** 2026-01-27 | **State:** open

---

## Description

The `/keystore` directory in the repository contains test keystores, private keys, certificates, and plaintext keystore passwords committed to version control. While these are used only for testing and do not represent production secrets, their presence:

- Creates confusion about whether they are safe to use in production.
- Inflates repository size and history.
- Sets a poor precedent for secrets management.
- Prevents rotation — the keys are shared across all developers and CI environments.

---

## Acceptance Criteria

- No `*.jks`, `*.p12`, `*.pem`, or `*.key` files from `/keystore` are present in the committed git history going forward (existing history may remain).
- `./gradlew clean test` passes on a clean checkout where the `/keystore` directory does not exist.
- All SSL-related integration tests continue to pass using keystores generated at build time.
- Keystore passwords are not stored in any committed file; they are defined as a well-known test constant in a shared test utility class or generated alongside the keystore.
- A CI check fails the build if any keystore/key files are committed to the repository in the future.

---

## Technical Solution

### Files to change

- New: `gradle/generate-test-keystores.gradle` — Gradle task script
- `build.gradle` (root and per-module) — register new task as dependency of `processTestResources` and `test`
- All `src/test/resources/application.yml` files referencing `/keystore` paths — update to `${buildDir}/keystore/`
- All `@Value("${server.ssl.keyStore:...}")` references in test classes — point to generated paths
- `.github/workflows/*.yml` — add CI check for committed keystore files
- Delete committed keystore files from `/keystore/` after audit

### Changes

**Step 1 — Audit all references**

Run `grep -r '/keystore' --include='*.yml' --include='*.java' --include='*.properties' .` across all modules. Document every reference before deleting anything.

**Step 2 — New Gradle task `:generateTestKeystores`**

```groovy
// gradle/generate-test-keystores.gradle
task generateTestKeystores {
    def outputDir = file("${buildDir}/keystore")
    outputs.dir outputDir
    doLast {
        outputDir.mkdirs()
        exec {
            commandLine 'keytool', '-genkeypair',
                '-alias', 'localhost',
                '-keyalg', 'RSA', '-keysize', '2048',
                '-validity', '3650',
                '-keystore', "${outputDir}/localhost.p12",
                '-storetype', 'PKCS12',
                '-storepass', 'password',
                '-dname', 'CN=localhost,O=Zowe,C=US'
        }
        // Export cert for trust store population
        exec {
            commandLine 'keytool', '-exportcert',
                '-alias', 'localhost',
                '-keystore', "${outputDir}/localhost.p12",
                '-storetype', 'PKCS12',
                '-storepass', 'password',
                '-file', "${outputDir}/localhost.cer",
                '-rfc'
        }
    }
}

processTestResources.dependsOn generateTestKeystores
test.dependsOn generateTestKeystores
```

Declare `inputs.files` / `outputs.dir` for Gradle up-to-date checking so the task only runs when the build directory is clean.

**Step 3 — Shared test password constant**

```java
// apiml-common/src/test/java/.../TestKeystoreConstants.java
public final class TestKeystoreConstants {
    public static final String TEST_KEYSTORE_PASSWORD = "password";
    public static final String TEST_KEYSTORE_PATH =
        System.getProperty("project.buildDir", "build") + "/keystore/localhost.p12";
    private TestKeystoreConstants() {}
}
```

**Step 4 — CI guard**

```yaml
# .github/workflows/ci.yml addition
- name: Check no keystore files committed
  run: |
    if git ls-files keystore/ | grep -qE '\.(jks|p12|pem|key|cer)$'; then
      echo "ERROR: Keystore/key files must not be committed to the repository"
      git ls-files keystore/ | grep -E '\.(jks|p12|pem|key|cer)$'
      exit 1
    fi
```

### Tests

- **Acceptance test**: run `./gradlew clean test` after deleting the `/keystore` directory from the working tree and assert the build is green — this is the primary acceptance criterion.
- **Regression guard**: all existing SSL integration tests in `integration-tests/` must pass with generated keystores, confirming functional equivalence.
- **CI enforcement**: the GitHub Actions `git ls-files` check (above) is itself the automated test for this issue — it will catch any future accidental keystore commits.
