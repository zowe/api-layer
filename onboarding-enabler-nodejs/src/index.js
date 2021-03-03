/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const Eureka = require('eureka-js-client').Eureka;
const yaml = require('js-yaml');
const fs = require('fs');

let certFile = null;
let keyFile = null;
let caFile = null;
let passPhrase = null;

/**
 * Read ssl service configuration
 */
function readTlsProps() {
    try {
        const config = yaml.load(fs.readFileSync('config/service-configuration.yml', 'utf8'));
        certFile = config.ssl.certificate;
        keyFile = config.ssl.keystore;
        caFile = config.ssl.caFile;
        passPhrase = config.ssl.keyPassword;

    } catch (e) {
        console.log(e);
    }
}
readTlsProps();

let tlsOptions = {
    cert: fs.readFileSync(certFile),
    key: fs.readFileSync(keyFile),
    passphrase: passPhrase,
    ca: fs.readFileSync(caFile)
};

const client = new Eureka({
    filename: 'service-configuration',
    cwd: 'config/',
    requestMiddleware: (requestOpts, done) => {
        done(Object.assign(requestOpts, tlsOptions));
    }
});

/**
 * Function that uses the eureka-js-client library to register the application to Eureka
 */
function connectToEureka() {
    client.start(function(error) {
        if (error != null) {
            console.log(JSON.stringify(error));
        }
    });
}

/**
 * Unregister the Eureka client from Eureka (i.e. when the application down)
 */
function unregisterFromEureka() {
    console.log("\nUnregistering the service from Eureka...")
    client.stop();
}

connectToEureka();

module.exports = {connectToEureka, tlsOptions, unregisterFromEureka};



