# Integration tests

Integration tests are meant to run against mocked version of services and should verify the overall integrity of the app.

## Used technologies

For our message tests we use [jest](https://jestjs.io/) as the test runner and reporter, [react-testing-library](https://github.com/kentcdodds/react-testing-library) as the DOM API and [express](https://expressjs.com/) for mocked backend services.

## How to run the integration tests

To be able to run the tests you need to first run the mocked-backend service (`npm run start:mocked-backend`). 

Then run the tests itself (`npm run test:message`).

## Coverage

When you run `npm run coverage` the computed coverage includes coverage from message tests.
