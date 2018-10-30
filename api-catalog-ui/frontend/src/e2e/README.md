# e2e tests

## Running the e2e tests locally

You need to have a running instance of the API Catalog that you want to test online.

Add `REACT_APP_CATALOG_URL_TEST` equal to the URL of your running instance (for example `https://localhost:10010/ui/v1/apicatalog`) to the [test env file](../../.env.test).

Then in another terminal run `npm run test:e2e`.

**Note:** Example must be run from the bundled UI in the resources/static in the api-catalog-services module (not dev mode , not mocked backed)
**Note:** coverage is not collected from e2e tests!
