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

/* eslint-disable no-unused-expressions */
import { expect } from 'chai';
import merge from 'lodash/merge.js';

import ConfigClusterResolver from '../src/ConfigClusterResolver.js';

function makeConfig(overrides = {}) {
  const config = {
    instance: {
      dataCenterInfo: { metadata: { 'availability-zone': '1b' } },
    },
    eureka: {
      maxRetries: 0,
      ec2Region: 'my-region',
    },
  };
  return merge({}, config, overrides);
}

describe('Config Cluster Resolver', () => {
  describe('resolveEurekaUrl() with host/port config', () => {
    let resolver;
    beforeEach(() => {
      resolver = new ConfigClusterResolver(makeConfig({
        eureka: {
          host: 'eureka.mydomain.com',
          servicePath: '/eureka/v2/apps/',
          port: 9999,
        },
      }));
    });

    it('should return base Eureka URL using configured host', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://eureka.mydomain.com:9999/eureka/v2/apps/');
      });
    });
  });

  describe('resolveEurekaUrl() with default serviceUrls', () => {
    let resolver;
    beforeEach(() => {
      resolver = new ConfigClusterResolver(makeConfig({
        eureka: {
          serviceUrls: {
            default: [
              'http://eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
          },
        },
      }));
    });

    it('should return first Eureka URL from configured serviceUrls', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://eureka1.mydomain.com:9999/eureka/v2/apps/');
      });
    });

    it('should return next Eureka URL from configured serviceUrls', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://eureka2.mydomain.com:9999/eureka/v2/apps/');
        // next attempt should still be the next server
        resolver.resolveEurekaUrl((errTwo, eurekaUrlTwo) => {
          expect(eurekaUrlTwo).to.equal('http://eureka2.mydomain.com:9999/eureka/v2/apps/');
        });
      }, 1);
    });
  });

  describe('resolveEurekaUrl() with zoned serviceUrls', () => {
    let resolver;
    beforeEach(() => {
      resolver = new ConfigClusterResolver(makeConfig({
        eureka: {
          availabilityZones: {
            'my-region': ['1a', '1b', '1c'],
          },
          serviceUrls: {
            '1a': [
              'http://1a-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1a-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1a-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
            '1b': [
              'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
            '1c': [
              'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
          },
        },
      }));
    });

    it('should return first Eureka URL from configured serviceUrls', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://1a-eureka1.mydomain.com:9999/eureka/v2/apps/');
      });
    });
  });

  describe('resolveEurekaUrl() with zoned serviceUrls and preferSameZone', () => {
    let resolver;
    beforeEach(() => {
      resolver = new ConfigClusterResolver(makeConfig({
        eureka: {
          preferSameZone: true,
          availabilityZones: {
            'my-region': ['1a', '1b', '1c'],
          },
          serviceUrls: {
            '1a': [
              'http://1a-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1a-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1a-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
            '1b': [
              'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
            '1c': [
              'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
              'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
            ],
          },
        },
      }));
    });

    it('should return first Eureka URL from configured serviceUrls', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/');
      });
    });
  });

  describe('resolveEurekaUrl(), zoned serviceUrls, preferSameZone, missing dataCenterInfo', () => {
    let resolver;
    const config = {
      instance: {},
      eureka: {
        maxRetries: 0,
        ec2Region: 'my-region',
        preferSameZone: true,
        availabilityZones: {
          'my-region': ['1a', '1b', '1c'],
        },
        serviceUrls: {
          '1a': [
            'http://1a-eureka1.mydomain.com:9999/eureka/v2/apps/',
            'http://1a-eureka2.mydomain.com:9999/eureka/v2/apps/',
            'http://1a-eureka3.mydomain.com:9999/eureka/v2/apps/',
          ],
          '1b': [
            'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
            'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
            'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
          ],
          '1c': [
            'http://1b-eureka1.mydomain.com:9999/eureka/v2/apps/',
            'http://1b-eureka2.mydomain.com:9999/eureka/v2/apps/',
            'http://1b-eureka3.mydomain.com:9999/eureka/v2/apps/',
          ],
        },
      },
    };
    beforeEach(() => {
      resolver = new ConfigClusterResolver(config);
    });

    it('should return first Eureka URL from configured serviceUrls', () => {
      resolver.resolveEurekaUrl((err, eurekaUrl) => {
        expect(eurekaUrl).to.equal('http://1a-eureka1.mydomain.com:9999/eureka/v2/apps/');
      });
    });
  });
});
