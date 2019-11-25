# React/Mineral UI based API Catalog UI

Contains React component loaded at startup which calls the running API Catalog services to get a list of registered Tiles and displays them in a list.


## Testing

The front end is covered by Unit tests, integration tests and e2e tests. The testing part of the CI/CD pipeline, but manual testing
can be done locally. 

#### Unit and Integration testing

Unit tests for each component and integration tests are performed part of the CI/CD ,and you can see the results of the tests as well as the coverage report
at stage "Javascript Test and Coverage"

#### For Manual testing

You need to have Node.js installed. Run command `npm install` in your root directory to install a package, and any packages that it depends on
Either use directly node.js command ``npm test`` while on directory /api-catalog-ui/frontend or via gradle task `api-catalog-ui:runTests`. 
For coverage use ``npm run coverage`` or gradle task "javaScriptCoverage"



## Installation

1. Clone the repo

2. Install the package manager `pnpm` by running `npm add -g pnpm`

3. run `pnpm install` inside the repo folder to install all dependencies

3. run `npm run start` to run the live reload server (will automatically open new browser tab on <https://localhost:3000/> where the app runs)

## Testing

### Unit tests

For Unit tests we use [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

When virtually rendering components you should use `.shallow`.

To run all unit and integration tests run `npm test`.

The tests will auto rerun after you make any file changes to test files.

### Integration tests

For e2e tests we use [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

When virtually rendering components you should use `.mount`.

[Integration tests](src/integration-tests) are meant to verify the overall functionality of the app, so they should be run with mocked backend as a source for info.

### e2e tests

For e2e tests we use [puppeteer](https://github.com/GoogleChrome/puppeteer), [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

We are using [puppeteer](https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md) to controll headless chrome and jest for assertions.

to run e2e tests follow these steps:

1. Have some running instance to test (for example `npm start`)

2. Point e2e to the instance to the instance you want to test

3. Run the tests with `npm run test:e2e`

**Note:** coverage is not collected from e2e tests

**Note:** you should run the real catalog services as a backend for e2e testing

### Automated testing of responsive design

Check [galen-tests](./src/responsive-tests/galen-tests.MD) to know how to run responsive tests.

### Code coverage

To get the current coverage run `npm run coverage`.

You can see the coverage in the terminal window or go to the `coverage/lcov-report` folder and open the `index.html` file.

## Dev environment

For local development run `npm run start:dev`.

Using this command you will have an instance of catalog UI running on <http://localhost:3000/> using mocked backend for info updates.

To configure variouse environment specific variables for development see [this env file](./.env.development);

## Mocked backend

If you want to develop only on the UI side mocked backend can come in handy. It serves a static jsons from the `mocked-abackend/assets` folder.

To fire up the server run `npm run start:mocked-backend`;

The server tuns on <http://localhost:8000/> and the endpoints addresses are set up so that they simulate the real services as much as possible.

## Building

To build the UI run `npm run build`.

This will generate `build` folder.

You can install a tool called _serve_ (`npm install -g serve`) and then run `serve -s build` to start a static server.

## Cypress tests

The tests are located in api-catalog-ui/frontend/cypress/integration. Inside this folder you can find two other folders:

`/mocked-e2e` - e2e tests that are supposed to run locally against mocked backend
`/e2e` - e2e tests that can be run locally but they should mostly run as a part of our pipeline
Both can be run in interactive mode (more on that bellow).

All commands related to cypress start with cy.
As of now the available ones are:

`cy:open` - opens the interactive window with no environment variables set
`cy:e2e:ci` - runs all test inside the e2e folder testing the instance in the baseURL env variable while using credential passed as parameters (This command should be used in the pipeline)
`cy:e2e:localhost` - runs all test inside the e2e folder testing the localhost instance
`cy:e2e:mocked-backend` - runs all test inside the /integration/integration folder; integration tests should run locally against mocked backend


