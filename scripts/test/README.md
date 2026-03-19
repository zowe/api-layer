# Shell Script Tests

This directory contains BATS (Bash Automated Testing System) tests for the APIML shell scripts.

## Prerequisites

Install BATS:

```bash
# macOS
brew install bats-core

# Ubuntu/Debian
apt install bats

# Or install from source
git clone https://github.com/bats-core/bats-core.git
cd bats-core
./install.sh /usr/local
```

## Running Tests

From the project root:

```bash
# Run all tests
bats scripts/test/start_test.bats

# Run with verbose output
bats --verbose-run scripts/test/start_test.bats

# Run with TAP output
bats --tap scripts/test/start_test.bats

# Run specific test by name pattern
bats --filter "parse_jvm_args" scripts/test/start_test.bats
bats --filter "apiml-common-scripts" scripts/test/start_test.bats
```

## Writing New Tests

BATS tests follow this structure:

```bash
@test "description of what is being tested" {
    # Setup - set environment variables
    export SOME_VAR="value"
    
    # Execute - source the script
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"
    
    # Assert - verify variables are set correctly
    [ "$result_var" = "expected" ]
}
```

Common assertions:
- `[ "$var" = "value" ]` - exact string match
- `[ -z "$var" ]` - variable is empty
- `[ -n "$var" ]` - variable is not empty
- `[[ "$var" == *"pattern"* ]]` - pattern match
- `type function_name` - verify function exists

## Test Structure

Each test:
1. Sets up required environment variables in `setup()`
2. May override specific variables for the test case
3. Sources the actual script being tested
4. Verifies the script populated variables correctly
5. Cleans up in `teardown()`
