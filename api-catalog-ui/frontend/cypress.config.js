const { defineConfig } = require('cypress')

module.exports = defineConfig({
  env: {
    catalogHomePage: 'https://localhost:10010/apicatalog/ui/v1',
    viewportWidth: 1400,
    viewportHeight: 980,
    username: 'USER',
    password: 'validPassword',
  },
  reporter: 'junit',
  defaultCommandTimeout: 30000,
  reporterOptions: {
    mochaFile: 'test-results/e2e/output-[hash].xml',
  },
  video: false,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.js')(on, config)
    },
  },
})
