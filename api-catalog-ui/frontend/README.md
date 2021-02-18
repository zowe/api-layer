# React/Mineral UI based API Catalog UI

Contains React component loaded at startup which calls the running API Catalog services to get a list of registered Tiles and displays them in a list.


## Testing

The front end is covered by Unit tests and e2e tests. The testing part of the CI/CD pipeline, but manual testing
can be done locally. 

#### Unit testing

Unit tests for each component are performed part of the api-catalog-ui build task.

#### For Manual testing

You need to have Node.js installed. Run command `npm install` in your root directory to install a package, and any packages that it depends on
Either use directly node.js command ``npm test`` while on directory /api-catalog-ui/frontend. 
For coverage use ``npm run coverage`` or gradle task "javaScriptCoverage"


## Installation

You need to have Node.js installed.

1. Clone the repo

2. Run `npm install` inside the repo folder to install all dependencies

3. (Optional) run `npm run start` to run the live reload server (will automatically open new browser tab on <https://localhost:3000/> where the app runs)

## Testing

### Unit tests

For Unit tests we use [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

To run all unit tests run `npm test`.

### e2e tests

For e2e tests we use [Cypress](https://github.com/cypress-io/cypress), [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

We are using [cypress](https://github.com/cypress-io/cypress) to control headless chrome and jest for assertions.

to run e2e tests follow these steps:

1. Have some running instance to test (for example `npm run api-layer-ci` from the repository root)

2. Point e2e to the instance to the instance you want to test

3. Run the tests with `npm run cy:e2e:ci`

**Note:** coverage is not collected from e2e tests

**Note:** you should run the real catalog services as a backend for e2e testing

### Code coverage

To get the current coverage run `npm run coverage`.

You can see the coverage in the terminal window or go to the `coverage/lcov-report` folder and open the `index.html` file. 

Coverage is being produced as part of every `npm run build`.

## Dev environment

**!! Not maintained, use at your own risk !!**

For local development run `npm run start:dev`.

Using this command you will have an instance of catalog UI running on <https://localhost:3000/> using mocked backend for info updates.

To configure variouse environment specific variables for development see [this env file](./.env.development);

## Mocked backend

**!! Not maintained, use at your own risk !!**

If you want to develop only on the UI side mocked backend can come in handy. It serves a static jsons from the `mocked-abackend/assets` folder.

To fire up the server run `npm run start:mock`;

The server tuns on <http://localhost:8000/> and the endpoints addresses are set up so that they simulate the real services as much as possible.

## Building

To build the UI run `npm run build`. Unit tests are run on every build. Coverage is produced on every build.

This will generate `build`, `coverage` and `test-results` folders.

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


