/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import Eureka from './EurekaClient.js';
import yaml from 'js-yaml';
import * as fs from 'fs';

let certFile = null;
let keyFile = null;
let caFile = null;
let passPhrase = null;
let client = null;
let tlsOpts = null;

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

// eslint-disable-next-line import/no-mutable-exports
export let tlsOptions = tlsOpts;

function init() {
  const defaultFile = fs.existsSync('config/service-configuration.yml');
  if (defaultFile) {
    readTlsProps();
    tlsOpts = {
      cert: fs.readFileSync(certFile),
      key: fs.readFileSync(keyFile),
      passphrase: passPhrase,
      ca: fs.readFileSync(caFile),
    };
    client = new Eureka({
      filename: 'service-configuration',
      cwd: 'config/',
      requestMiddleware: (requestOpts, done) => {
        done(Object.assign(requestOpts, tlsOptions));
      },
    });
    tlsOptions = tlsOpts;
  }
}

init();

export function getTlsOptions() {
  return tlsOptions;
}

/**
 * Function that uses the eureka-js-client library to register the application to Eureka
 */
export function connectToEureka() {
  if (!client) {
    throw new Error('Eureka client not initialized');
  }
  client.start((error) => {
    if (error != null) {
      console.log(JSON.stringify(error));
    }
  });
}

/**
 * Unregister the Eureka client from Eureka (i.e. when the application down)
 */
export function unregisterFromEureka() {
  if (!client) {
    throw new Error('Eureka client not initialized');
  }
  console.log('\nUnregistering the service from Eureka...');
  client.stop();
}

export const EurekaClient = Eureka;
