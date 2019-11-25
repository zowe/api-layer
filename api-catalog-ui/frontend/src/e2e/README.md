# e2e tests

## Running the e2e tests locally

You need to have a running instance of the API Catalog that you want to test online.

Set `baseUrl` the URL of your running instance (for example `https://localhost:10010/ui/v1/apicatalog`). This environment variable is defined in  the [cypress.json file](../../cypress.json).

Set `username` and `password` to you mainframe password in case when you are using real backend and when you're not using Dummy provider.

Then in another terminal, run `npm run cy:e2e:localhost`.

For more information about e2e tests, check [this file](../../README.md)

**Note:** Example must be run from the bundled UI in the resources/static in the api-catalog-services module (not dev mode, not mocked backend)

**Note:** Unit test coverage is not collected from e2e tests
