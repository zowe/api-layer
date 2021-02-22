# React/Mineral UI based API Catalog UI

Contains React component loaded at startup which calls the running API Catalog services to get a list of registered Tiles and displays them in a list.

## Install Node

Many of the tasks here can be run either through Gradle or through NPM. If you're using npm directly, ensure you have correct version of node installed globally. The version of node can be found in `gradle.properties` in variable `nodejsVersion`. You have to perform `npm install` command from the `api-catalog-ui/frontend` directory first, to download necessary dependencies before running any `npm` commands. 

For the gradle tasks, the node-gradle plugin is used which downloads correct node version and sets up dependencies automatically. This approach is used in CI environment but can be used locally as well.

## Testing

The front end is covered by Unit tests and e2e tests. The testing part of the CI/CD pipeline, but manual testing can be done locally. 

### Unit tests

Unit tests for each component are performed part of the api-catalog-ui build task.

For Unit tests we use [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

#### For Manual testing

Npm: `npm test` while on directory `/api-catalog-ui/frontend`.
Gradle: `./gradlew api-catalog-ui:test`

For coverage
Npm: `npm run coverage`
Gradle: `./gradlew api-catalog-ui:build`

### e2e tests

For e2e tests we use [Cypress](https://github.com/cypress-io/cypress), [Enzyme](https://github.com/airbnb/enzyme) and [jest](https://jestjs.io/).

We are using [cypress](https://github.com/cypress-io/cypress) to control headless chrome and jest for assertions.

to run e2e tests follow these steps:

1. Have some running instance to test (for example local instance `npm run api-layer-ci` from the repository root or some remote instance)

2. Run the e2e tests
   
    - against localhost:
      
    `npm run cy:e2e:localhost`

    - against real instance:
  

    ./gradlew api-catalog-ui:npmE2ECI -Dcredentials.user=${USERID} -Dcredentials.password=${PASSWORD} -Ddiscovery.host=${HOST} -Ddiscovery.port=${DISCOVERY_PORT} -Ddiscovery.scheme=${DISCOVERY_SCHEME} -Ddiscovery.instances=${DISCOVERY_NUM_INSTANCES} -Dgateway.instances=${GATEWAY_NUM_INSTANCES} -Dgateway.host=${HOST} -Dgateway.port=${GATEWAY_PORT} -Dgateway.externalPort=${GATEWAY_PORT} -Dgateway.internalPorts=${GATEWAY_PORT} -Dgateway.scheme=https

**Note:** coverage is not collected from e2e tests

**Note:** you should run the real catalog services as a backend for e2e testing

### Code coverage

To get the current coverage run `npm run coverage` or Gradle `build` task.

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


