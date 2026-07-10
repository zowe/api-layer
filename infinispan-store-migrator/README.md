# Store migrator

Poc to showcase migration of the only distributed cache:

- `invalidatedJwtTokens`
- `zoweCache`
- `zoweInvalidatedTokenCache`

using the StoreMigrator Utility class from Infinispan Tools.

# Infinispan CLI

1. Installation (macOS):

    ```shell
    brew tap infinispan/tap 
    brew install infinispan-cli
    ```

2. Usage:

    ```shell
    infinispan-cli migrate store -p <path_to>/migrator.properties
    ```

Both CLI and StoreMigrator use the `migration.properties` files for the migration.


