/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Jacob Quatier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import Eureka from '../src/EurekaClient.js';
import { expect } from 'chai';
import fs from 'fs';

describe('Integration Test', () => {
  const config = {
    instance: {
      app: 'jqservice',
      hostName: 'localhost',
      ipAddr: '127.0.0.1',
      port: {
        $: 8080,
        '@enabled': true,
      },
      vipAddress: 'jq.test.something.com',
      instanceId: 'localhost:hwexpress:8080',
      dataCenterInfo: {
        '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
        name: 'MyOwn',
      },
    },
    eureka: {
      heartbeatInterval: 30000,
      registryFetchInterval: 5000,
      fetchRegistry: true,
      waitForRegistry: true,
      servicePath: '/eureka/v2/apps/',
      ssl: true,
      useDns: false,
      fetchMetadata: true,
      host: 'localhost',
      port: 10011,
    },
    ssl: {
      certificate: '../keystore/localhost/localhost.keystore.cer',
      keystore: '../keystore/localhost/localhost.keystore.key',
      caFile: '../keystore/localhost/localhost.pem',
      keyPassword: 'password',
    },
    requestMiddleware: (requestOpts, done) => {
      done(Object.assign(requestOpts, {
        cert: fs.readFileSync(config.ssl.certificate),
        key: fs.readFileSync(config.ssl.keystore),
        passphrase: config.ssl.keyPassword,
        ca: fs.readFileSync(config.ssl.caFile),
      }));
    },
  };

  const client = new Eureka(config);
  before((done) => {
    client.start(done);
  });

  it('should be able to get instance by the app id', () => {
    const instances = client.getInstancesByAppId(config.instance.app);
    expect(instances.length).to.equal(1);
  });

  it('should be able to get instance by the vipAddress', () => {
    const instances = client.getInstancesByVipAddress(config.instance.vipAddress);
    expect(instances.length).to.equal(1);
  });

  after((done) => {
    client.stop(done);
  });
});
