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
```

## Writing New Tests

BATS tests follow this structure:

```bash
@test "description of what is being tested" {
    # Setup
    export SOME_VAR="value"
    
    # Execute
    result=$(some_command)
    
    # Assert
    [ "$result" = "expected" ]
    # or
    [[ "$result" == *"partial match"* ]]
}
```

Common assertions:
- `[ "$var" = "value" ]` - exact string match
- `[ -z "$var" ]` - variable is empty
- `[ -n "$var" ]` - variable is not empty
- `[[ "$var" == *"pattern"* ]]` - pattern match
- `[ "$status" -eq 0 ]` - exit status check

