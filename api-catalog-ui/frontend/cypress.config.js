/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const { defineConfig } = require('cypress');

module.exports = defineConfig({
    env: {
        catalogHomePage: 'https://localhost:10010/apicatalog/ui/v1',
        loginUrl: 'https://localhost:10010/apicatalog/api/v1/auth/login',
        gatewayOktaRedirect:
            'https://localhost:10010/gateway/oauth2/authorization/auth0?returnUrl=https%3A%2F%2Flocalhost%3A10010%2Fapplication',
        username: 'USER',
        password: 'validPassword',
        microservices: true,
},
    viewportWidth: 1400,
    viewportHeight: 980,
    chromeWebSecurity: false,
    experimentalSessionAndOrigin: true,
    reporter: 'junit',
    defaultCommandTimeout: 30000,
    reporterOptions: {
        mochaFile: 'test-results/e2e/output-[hash].xml',
    },
    video: false,
    e2e: {
        browser: 'chrome',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        // We've imported your old cypress plugins here.
        // You may want to clean this up later by importing these.
        setupNodeEvents(on, config) {
            // eslint-disable-next-line global-require
            return require('./cypress/plugins/index.js')(on, config);
        },
    },
});
