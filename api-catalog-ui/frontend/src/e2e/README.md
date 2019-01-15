# e2e tests

## Running the e2e tests locally

You need to have a running instance of the API Catalog that you want to test online.

Add `REACT_APP_CATALOG_URL_TEST` equal to the URL of your running instance (for example `https://localhost:10010/ui/v1/apicatalog`) to the [test env file](../../.env.test).

Set `REACT_APP_CATALOG_USERNAME` and `REACT_APP_CATALOG_PASSWORD` to you mainframe password in case when you are using real backend.

Then in another terminal, run `npm run test:e2e`.

If you want to see what is going on, set value of `headless` to `false` in `api-catalog-ui/frontend/src/e2e/e2e.test.jsx`.

**Note:** Example must be run from the bundled UI in the resources/static in the api-catalog-services module (not dev mode, not mocked backend)

**Note:** Unit test coverage is not collected from e2e tests
