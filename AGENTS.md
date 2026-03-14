# AGENTS.md - Zowe API Mediation Layer Developer Guide

This file provides guidelines for agentic coding agents operating in this repository.

## Project Overview

The Zowe API Mediation Layer is a Java/Spring Boot monorepo that provides API gateway, service discovery, and API catalog functionality. It uses Gradle as the build system and JUnit 5 for testing.

- **Language**: Java 17
- **Build Tool**: Gradle (wrapper provided)
- **Test Framework**: JUnit 5 (Jupiter)
- **Key Services**: Gateway, Discovery, API Catalog, Caching, ZAAS

---

## Build Commands

### Build All
```bash
./gradlew build
```

### Build Specific Module
```bash
./gradlew :gateway-service:build
./gradlew :discovery-service:build
./gradlew :api-catalog-services:build
```

### Run Unit Tests
```bash
./gradlew test                    # All unit tests
./gradlew :gateway-service:test   # Single module tests
./gradlew :gateway-service:test --tests "org.zowe.apiml.gateway.*"  # Specific test class
```

### Run Single Test
```bash
./gradlew :module-name:test --tests "fully.qualified.TestClassName.methodName"
```

Example:
```bash
./gradlew :gateway-service:test --tests "org.zowe.apiml.gateway.filters.post.SsocookieFilterTest"
```

### Run Integration Tests
```bash
npm run test                      # Run all integration tests
npm run test:local               # Run local integration tests
```

### Run with Code Quality Checks
```bash
./gradlew check                   # Runs checkstyle + tests
./gradlew checkstyleMain          # Checkstyle for main code
./gradlew checkstyleTest          # Checkstyle for test code
```

### Other Useful Commands
```bash
./gradlew clean                   # Clean build artifacts
./gradlew build -x test          # Build without tests
./gradlew dependencies           # Show dependencies
./gradlew buildZoweServer        # Build only main server components
./gradlew buildEnablers          # Build SDK enablers
```

---

## Code Style Guidelines

### General
- **Indentation**: 4 spaces (not tabs)
- **Max Line Length**: 120 characters
- **Charset**: UTF-8
- **Line Endings**: LF (Unix-style)
- **EditorConfig**: Use `.editorconfig` - most IDEs support it

### Naming Conventions
- **Packages**: Lowercase, single words by feature (e.g., `security`, `message`, `config`)
- **Classes**: PascalCase (e.g., `GatewaySecurityService`)
- **Methods**: camelCase (e.g., `getAuthenticationToken`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
- **Master Package**: `org.zowe.apiml`

### File Organization
- Keep package hierarchy shallow
- One public class per file (filename matches class name)
- Source: `src/main/java/`
- Tests: `src/test/java/`

### Java Specific
- Use Java 17 features where appropriate
- Use Lombok for boilerplate (see `lombok.config`)
- Prefer composition over inheritance
- Keep classes small, follow Single Responsibility Principle

### Code Quality Enforcement
- **Checkstyle**: Enforced via Gradle (`gradle/code-quality.gradle`)
- Config file: `codequality/checkstyle/checkstyle.xml`
- Run checkstyle: `./gradlew checkstyleMain`

---

## Error Handling

- Use meaningful exception names
- Provide adequate logging for debugging production issues
- Reuse existing logging and error handling patterns
- Use `StorageException` patterns from existing code as examples

---

## Testing Guidelines

### Test Structure
- Use JUnit 5 (Jupiter) for new tests
- Follow `given_when_then` naming pattern for test methods
- Use `@Nested` for grouping related tests
- Example: [CachingControllerTest.java](caching-service/src/test/java/org/zowe/apiml/caching/api/CachingControllerTest.java)

### Test Method Naming
```java
@Nested
class DeleteKeyUnitTests {
    @Test
    void givenValidKey_whenDelete_thenReturnOk() { }
}
```

### Test Coverage
- Minimum 80% code coverage for new code
- Coverage should not decrease from master
- SonarCloud quality gate must pass
- No new code smells, security hotspots, or bugs

### Test Dependencies
- Mock with Mockito
- Use Spring Boot Test utilities
- Integration tests use RestAssured

---

## Documentation

- Javadoc required for public APIs
- Keep packages independent without cross-dependencies
- Add README.md for new modules (build/run instructions)
- Comment complex logic for future maintainers

---

## Commit Message Format

Follow Conventional Commits:

```
<type>[optional scope]: <description>

[optional body]

[footer(s)]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `chore`: Maintenance, dependencies

Example:
```
feat(authentication): Add x509 certificate support

This commit adds support for x509 certificate authentication
as an alternative to JWT tokens.

Signed-off-by: John Doe <john.doe@zowe.org>
BREAKING CHANGE: Authentication header format changed
```

---

## Important Directories

| Directory | Purpose |
|-----------|---------|
| `gateway-service/` | API Gateway service |
| `discovery-service/` | Service Discovery service |
| `api-catalog-services/` | API Catalog backend |
| `api-catalog-ui/` | API Catalog frontend (Node.js) |
| `caching-service/` | Caching service |
| `zaas-service/` | Zowe Authentication Service |
| `apiml-common/` | Shared library |
| `integration-tests/` | Integration tests |
| `config/local/` | Local configuration samples |

---

## Node.js/TypeScript Guidelines

For the API Catalog UI and onboarding-enabler-nodejs:

- Follow Airbnb JavaScript Style Guide
- ESLint config: `onboarding-enabler-nodejs/.eslintrc`
- Run: `npm install`, `npm test`

---

## Pull Request Requirements

1. Every PR must have an associated GitHub issue
2. PRs should be small and focused
3. Include unit tests for all new code
4. Add integration tests where needed
5. Run `./gradlew check` locally before submitting
6. Ensure SonarCloud quality gate passes
7. Sign off commits (`Signed-off-by: Your Name <email>`)

---

## IDE Setup

- Use IntelliJ IDEA with:
  - Checkstyle plugin
  - EditorConfig plugin
  - Lombok plugin
- Import settings from `settings.gradle`
