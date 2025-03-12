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

/* eslint-disable no-unused-expressions, max-len */
import sinon from 'sinon';
import * as chai from 'chai';
const expect = chai.expect;
import sinonChai from 'sinon-chai';
import { EventEmitter } from 'events';
import { join } from 'path';
import merge from 'lodash/merge.js';
import https from 'https';

import Eureka from '../src/EurekaClient.js';
import DnsClusterResolver from '../src/DnsClusterResolver.js';

chai.use(sinonChai);

function makeConfig(overrides = {}) {
  const config = {
    instance: {
      app: 'app',
      vipAddress: '1.2.2.3',
      hostName: 'myhost',
      port: 9999,
      dataCenterInfo: {
        name: 'MyOwn',
      },
    },
    eureka: { host: '127.0.0.1', port: 9999, maxRetries: 0 },
  };
  return merge({}, config, overrides);
}

function mockSuccessfulResponse(accumulator, statusCode) {
  const res = {
    statusCode: statusCode || 204,
    callback: [],
    on: (event, callback) => {
      console.log(`EVENT ${event}`);
      res.callback[event] = callback;
    },
  };
  return sinon.stub(https, 'request').yields(res).returns({
    end: () => {
      if (statusCode === 200 && res.callback.data('{}'));
      if (res.callback.end) res.callback.end.apply();
    },
    write: (body) => {
      if (accumulator) accumulator.body = body;
    },
    on: () => {},
  });
}

describe('Eureka client', () => {
  describe('Eureka()', () => {
    it('should extend EventEmitter', () => {
      expect(new Eureka(makeConfig())).to.be.instanceof(EventEmitter);
    });

    it('should throw an error if no config is found', () => {
      function fn() {
        return new Eureka();
      }
      expect(fn).to.throw();
    });

    it('should construct with the correct configuration values', () => {
      function shouldThrow() {
        return new Eureka();
      }

      function noApp() {
        return new Eureka({
          instance: {
            vipAddress: true,
            port: true,
            dataCenterInfo: {
              name: 'MyOwn',
            },
          },
          eureka: {
            host: true,
            port: true,
          },
        });
      }

      function shouldWork() {
        return new Eureka({
          instance: {
            app: true,
            vipAddress: true,
            port: true,
            dataCenterInfo: {
              name: 'MyOwn',
            },
          },
          eureka: {
            host: true,
            port: true,
          },
        });
      }

      function shouldWorkNoInstance() {
        return new Eureka({
          eureka: {
            registerWithEureka: false,
            host: true,
            port: true,
          },
        });
      }

      expect(shouldThrow).to.throw();
      expect(noApp).to.throw(/app/);
      expect(shouldWork).to.not.throw();
      expect(shouldWorkNoInstance).to.not.throw();
    });

    it('should use DnsClusterResolver when configured', () => {
      const client = new Eureka({
        instance: {
          app: true,
          vipAddress: true,
          port: true,
          dataCenterInfo: {
            name: 'MyOwn',
          },
        },
        eureka: {
          host: true,
          port: true,
          useDns: true,
          ec2Region: 'my-region',
        },
      });
      expect(client.clusterResolver.constructor).to.equal(DnsClusterResolver);
    });

    it('should throw when configured to useDns without setting ec2Region', () => {
      function shouldThrow() {
        return new Eureka({
          instance: {
            app: true,
            vipAddress: true,
            port: true,
            dataCenterInfo: {
              name: 'MyOwn',
            },
          },
          eureka: {
            host: true,
            port: true,
            useDns: true,
          },
        });
      }
      expect(shouldThrow).to.throw(/ec2Region/);
    });

    it('should accept requestMiddleware', () => {
      const requestMiddleware = (opts) => opts;
      const client = new Eureka({
        requestMiddleware,
        instance: {
          app: true,
          vipAddress: true,
          port: true,
          dataCenterInfo: {
            name: 'MyOwn',
          },
        },
        eureka: {
          host: true,
          port: true,
          useDns: true,
          ec2Region: 'my-region',
        },
      });
      expect(client.requestMiddleware).to.equal(requestMiddleware);
    });
  });

  describe('get instanceId()', () => {
    it('should return the configured instance id', () => {
      const instanceId = 'test_id';
      const config = makeConfig({
        instance: {
          instanceId,
        },
      });
      const client = new Eureka(config);
      expect(client.instanceId).to.equal(instanceId);
    });

    it('should return hostname for non-AWS datacenters', () => {
      const config = makeConfig();
      const client = new Eureka(config);
      expect(client.instanceId).to.equal('myhost');
    });

    it('should return instance ID for AWS datacenters', () => {
      const config = makeConfig({
        instance: { dataCenterInfo: { name: 'Amazon', metadata: { 'instance-id': 'i123' } } },
      });
      const client = new Eureka(config);
      expect(client.instanceId).to.equal('i123');
    });
  });

  describe('start()', () => {
    let config;
    let client;
    let registerSpy;
    let fetchRegistrySpy;
    let heartbeatsSpy;
    let registryFetchSpy;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    afterEach(() => {
      registerSpy.restore();
      fetchRegistrySpy.restore();
      heartbeatsSpy.restore();
      registryFetchSpy.restore();
    });

    it('should call register, fetch registry, startHeartbeat and startRegistryFetches', (done) => {
      registerSpy = sinon.stub(client, 'register').callsArg(0);
      fetchRegistrySpy = sinon.stub(client, 'fetchRegistry').callsArg(0);
      heartbeatsSpy = sinon.stub(client, 'startHeartbeats');
      registryFetchSpy = sinon.stub(client, 'startRegistryFetches');
      const eventSpy = sinon.spy();
      client.on('started', eventSpy);

      client.start(() => {
        expect(registerSpy).to.have.been.calledOnce;
        expect(fetchRegistrySpy).to.have.been.calledOnce;
        expect(heartbeatsSpy).to.have.been.calledOnce;
        expect(registryFetchSpy).to.have.been.calledOnce;
        expect(eventSpy).to.have.been.calledOnce;
        done();
      });
    });

    it('should call fetch registry and startRegistryFetches when registration disabled', (done) => {
      config = makeConfig({
        eureka: {
          registerWithEureka: false,
        },
      });
      client = new Eureka(config);

      registerSpy = sinon.stub(client, 'register').callsArg(0);
      fetchRegistrySpy = sinon.stub(client, 'fetchRegistry').callsArg(0);
      heartbeatsSpy = sinon.stub(client, 'startHeartbeats');
      registryFetchSpy = sinon.stub(client, 'startRegistryFetches');
      const eventSpy = sinon.spy();
      client.on('started', eventSpy);

      client.start(() => {
        expect(registerSpy).to.not.have.been.called;
        expect(fetchRegistrySpy).to.have.been.calledOnce;
        expect(heartbeatsSpy).to.not.have.been.called;
        expect(registryFetchSpy).to.have.been.calledOnce;
        expect(eventSpy).to.have.been.calledOnce;
        done();
      });
    });

    it('should return error on start failure', (done) => {
      registerSpy = sinon.stub(client, 'register').yields(new Error('fail'));
      fetchRegistrySpy = sinon.stub(client, 'fetchRegistry').callsArg(0);
      heartbeatsSpy = sinon.stub(client, 'startHeartbeats');
      registryFetchSpy = sinon.stub(client, 'startRegistryFetches');
      const eventSpy = sinon.spy();
      client.on('started', eventSpy);

      client.start((error) => {
        expect(error).to.match(/fail/);
        expect(eventSpy).to.not.have.been.called;
        done();
      });
    });
  });

  describe('startHeartbeats()', () => {
    let config;
    let client;
    let renewSpy;
    let clock;
    before(() => {
      config = makeConfig();
      client = new Eureka(config);
      renewSpy = sinon.stub(client, 'renew');
      clock = sinon.useFakeTimers();
    });

    after(() => {
      renewSpy.restore();
      clock.restore();
    });

    it('should call renew on interval', () => {
      client.startHeartbeats();
      clock.tick(30000);
      expect(renewSpy).to.have.been.calledOnce;
      clock.tick(30000);
      expect(renewSpy).to.have.been.calledTwice;
    });
  });

  describe('startRegistryFetches()', () => {
    let config;
    let client;
    let fetchRegistrySpy;
    let clock;
    before(() => {
      config = makeConfig();
      client = new Eureka(config);
      fetchRegistrySpy = sinon.stub(client, 'fetchRegistry');
      clock = sinon.useFakeTimers();
    });

    after(() => {
      fetchRegistrySpy.restore();
      clock.restore();
    });

    it('should call renew on interval', () => {
      client.startRegistryFetches();
      clock.tick(30000);
      expect(fetchRegistrySpy).to.have.been.calledOnce;
      clock.tick(30000);
      expect(fetchRegistrySpy).to.have.been.calledTwice;
    });
  });

  describe('stop()', () => {
    let config;
    let client;
    let deregisterSpy;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
      deregisterSpy = sinon.stub(client, 'deregister').callsArg(0);
    });

    afterEach(() => {
      deregisterSpy.restore();
    });

    it('should call deregister', () => {
      const stopCb = sinon.spy();
      client.stop(stopCb);

      expect(deregisterSpy).to.have.been.calledOnce;
      expect(stopCb).to.have.been.calledOnce;
    });

    it('should skip deregister if registration disabled', () => {
      config = makeConfig({
        eureka: {
          registerWithEureka: false,
        },
      });
      client = new Eureka(config);

      const stopCb = sinon.spy();
      client.stop(stopCb);

      expect(deregisterSpy).to.not.have.been.called;
      expect(stopCb).to.have.been.calledOnce;
    });
  });

  describe('register()', () => {
    let config;
    let client;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    it('should trigger register event', (done) => {
      const requestStub = mockSuccessfulResponse();
      client.on('registered', () => {
        requestStub.restore();
        done();
      });
      client.register();
    });

    it('should call register URI', () => {
      const accumulator = {};
      const requestStub = mockSuccessfulResponse(accumulator);
      client.register();
      expect(JSON.parse(accumulator.body)).to.deep.equal({
        instance: {
          app: 'app',
          hostName: 'myhost',
          dataCenterInfo: { name: 'MyOwn' },
          port: 9999,
          status: 'UP',
          vipAddress: '1.2.2.3',
        },
      });
      requestStub.restore();
    });

    it('should throw error for non-204 response', () => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').yields({
        on: () => {},
        statusCode: 500,
      }).returns({
        on: (type, callback) => { callbacks[type] = callback; },
        end: () => { callbacks.error.apply(); },
        write: () => {},
      });

      const registerCb = sinon.spy();
      client.register(registerCb);

      expect(registerCb).to.have.been.calledWithMatch({
        message: 'eureka registration FAILED: status: 500 body: null',
      });
      requestStub.restore();
    });

    it('should throw error for request error', () => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').returns({
        on: (type, callback) => { callbacks[type] = callback; },
        write: () => {},
        end: () => {},
      });
      const registerCb = sinon.spy();
      client.register(registerCb);
      callbacks.error(new Error('request error'));

      expect(registerCb).to.have.been.calledWithMatch({ message: 'request error' });
      requestStub.restore();
    });
  });

  describe('deregister()', () => {
    let config;
    let client;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    it('should should trigger deregister event', () => {
      const requestStub = mockSuccessfulResponse();

      const eventSpy = sinon.spy();
      client.on('deregistered', eventSpy);
      client.register();
      client.deregister();

      requestStub.restore();
    });

    it('should call deregister URI', () => {
      const requestStub = mockSuccessfulResponse();
      client.deregister();

      const options = requestStub.resolvesArg(0).args[0][0];
      expect(options.method).to.be.equal('DELETE');
      expect(options.hostname).to.be.equal('127.0.0.1');
      expect(options.port).to.be.equal('9999');
      expect(options.path).to.be.equal('/eureka/v2/apps/app/myhost');

      requestStub.restore();
    });

    it('should throw error for non-200 response', () => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').yields({
        on: () => {},
        statusCode: 500,
      }).returns({
        on: (type, callback) => { callbacks[type] = callback; },
        end: () => { callbacks.error.apply(); },
        write: () => {},
      });

      const deregisterCb = sinon.spy();
      client.deregister(deregisterCb);

      expect(deregisterCb).to.have.been.calledWithMatch({
        message: 'eureka deregistration FAILED: status: 500 body: null',
      });
      requestStub.restore();
    });

    it('should throw error for request error', () => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').returns({
        on: (type, callback) => { callbacks[type] = callback; },
        write: () => {},
        end: () => {},
      });
      const deregisterCb = sinon.spy();
      client.deregister(deregisterCb);
      callbacks.error(new Error('request error'));

      expect(deregisterCb).to.have.been.calledWithMatch({ message: 'request error' });
      requestStub.restore();
    });
  });

  describe('renew()', () => {
    let config;
    let client;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    it('should call heartbeat URI', () => {
      const requestStub = mockSuccessfulResponse();
      client.renew();

      const options = requestStub.resolvesArg(0).args[0][0];
      expect(options.method).to.be.equal('PUT');
      expect(options.hostname).to.be.equal('127.0.0.1');
      expect(options.port).to.be.equal('9999');
      expect(options.path).to.be.equal('/eureka/v2/apps/app/myhost');

      requestStub.restore();
    });

    it('should trigger a heartbeat event', (done) => {
      const requestStub = mockSuccessfulResponse({}, 200);
      client.on('heartbeat', () => {
        requestStub.restore();
        done();
      });
      client.renew();
    });

    it('should re-register on 404', (done) => {
      const requestStub = sinon.stub(https, 'request');

      client.on('registered', () => {
        const postOptions = requestStub.resolvesArg(0).args[1][0];
        expect(postOptions.method).to.be.equal('POST');
        expect(postOptions.hostname).to.be.equal('127.0.0.1');
        expect(postOptions.port).to.be.equal('9999');
        expect(postOptions.path).to.be.equal('/eureka/v2/apps/app');

        const putOptions = requestStub.resolvesArg(0).args[0][0];
        expect(putOptions.method).to.be.equal('PUT');
        expect(putOptions.hostname).to.be.equal('127.0.0.1');
        expect(putOptions.port).to.be.equal('9999');
        expect(putOptions.path).to.be.equal('/eureka/v2/apps/app/myhost');

        requestStub.restore();
        done();
      });

      const callbacks = [];
      const req = {
        end: () => {},
        write: () => {},
        on: (type, callback) => {
          if (!callbacks[type]) callbacks[type] = callback;
        },
      };
      requestStub.yields({
        statusCode: 404,
        on: (type, callback) => {
          if (!callbacks[type]) callbacks[type] = callback;
        },
      }).returns(req);

      client.renew();

      requestStub.yields({
        statusCode: 204,
        on: () => {},
      }).returns(req);
      callbacks.end.apply();
    });
  });

  describe('eureka-client.yml', () => {
    let stub;
    let original;
    before(() => {
      original = `${process.cwd()}/test`;
      stub = sinon.stub(process, 'cwd').returns(original);
    });

    after(() => {
      stub.restore();
    });

    it('should load the correct', () => {
      const client = new Eureka(makeConfig());
      expect(client.config.eureka.custom).to.equal('test');
    });

    it('should load the environment overrides', () => {
      const client = new Eureka(makeConfig());
      expect(client.config.eureka.otherCustom).to.equal('test2');
      expect(client.config.eureka.overrides).to.equal(2);
    });

    it('should support a `cwd` and `filename` property', () => {
      const client = new Eureka(makeConfig({
        cwd: join(original, 'fixtures'),
        filename: 'config',
      }));
      expect(client.config.eureka.fromFixture).to.equal(true);
    });

    it('should throw error on malformed config file', () => {
      function malformed() {
        return new Eureka(makeConfig({
          cwd: join(__dirname, 'fixtures'),
          filename: 'malformed-config',
        }));
      }
      expect(malformed).to.throw(Error);
    });
    it('should not throw error on malformed config file', () => {
      function missingFile() {
        return new Eureka(makeConfig({
          cwd: join(original, 'fixtures'),
          filename: 'missing-config',
        }));
      }
      expect(missingFile).to.not.throw();
    });
  });

  describe('validateConfig()', () => {
    let config;
    beforeEach(() => {
      config = makeConfig({
        instance: { dataCenterInfo: { name: 'Amazon' } },
      });
    });

    it('should throw an exception with a missing instance.app', () => {
      function badConfig() {
        delete config.instance.app;
        return new Eureka(config);
      }
      expect(badConfig).to.throw(TypeError);
    });

    it('should throw an exception with a missing instance.vipAddress', () => {
      function badConfig() {
        delete config.instance.vipAddress;
        return new Eureka(config);
      }
      expect(badConfig).to.throw(TypeError);
    });

    it('should throw an exception with a missing instance.port', () => {
      function badConfig() {
        delete config.instance.port;
        return new Eureka(config);
      }
      expect(badConfig).to.throw(TypeError);
    });

    it('should throw an exception with a missing instance.dataCenterInfo', () => {
      function badConfig() {
        delete config.instance.dataCenterInfo;
        return new Eureka(config);
      }
      expect(badConfig).to.throw(TypeError);
    });

    it('should throw an exception with an invalid request middleware', () => {
      function badConfig() {
        config.requestMiddleware = 'invalid middleware';
        return new Eureka(config);
      }
      expect(badConfig).to.throw(TypeError);
    });
  });

  describe('getInstancesByAppId()', () => {
    let client;
    let config;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    it('should throw an exception if no appId is provided', () => {
      function noAppId() {
        client.getInstancesByAppId();
      }
      expect(noAppId).to.throw(Error);
    });

    it('should return a list of instances if appId is registered', () => {
      const appId = 'THESERVICENAME';
      const expectedInstances = [{ host: '127.0.0.1' }];
      client.cache.app[appId] = expectedInstances;
      const actualInstances = client.getInstancesByAppId(appId);
      expect(actualInstances).to.equal(expectedInstances);
    });

    it('should return empty array if no instances were found for given appId', () => {
      expect(client.getInstancesByAppId('THESERVICENAME')).to.deep.equal([]);
    });
  });

  describe('getInstancesByVipAddress()', () => {
    let client;
    let config;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
    });

    it('should throw an exception if no vipAddress is provided', () => {
      function noVipAddress() {
        client.getInstancesByVipAddress();
      }
      expect(noVipAddress).to.throw(Error);
    });

    it('should return a list of instances if vipAddress is registered', () => {
      const vipAddress = 'the.vip.address';
      const expectedInstances = [{ host: '127.0.0.1' }];
      client.cache.vip[vipAddress] = expectedInstances;
      const actualInstances = client.getInstancesByVipAddress(vipAddress);
      expect(actualInstances).to.equal(expectedInstances);
    });

    it('should return empty array if no instances were found for given vipAddress', () => {
      expect(client.getInstancesByVipAddress('the.vip.address')).to.deep.equal([]);
    });
  });

  describe('fetchRegistry()', () => {
    let config;
    let client;
    beforeEach(() => {
      config = makeConfig();
      client = new Eureka(config);
      sinon.stub(client, 'transformRegistry');
      sinon.stub(client, 'handleDelta');
    });

    afterEach(() => {
      client.transformRegistry.restore();
      client.handleDelta.restore();
    });

    it('should should trigger registryUpdated event', (done) => {
      const requestStub = mockSuccessfulResponse({}, 200);
      client.on('registryUpdated', () => {
        requestStub.restore();
        done();
      });
      client.fetchRegistry();
    });

    it('should call registry URI', (done) => {
      const requestStub = mockSuccessfulResponse({}, 200);

      client.fetchRegistry(() => {
        const options = requestStub.resolvesArg(0).args[0][0];
        expect(options.method).to.be.equal('GET');
        expect(options.hostname).to.be.equal('127.0.0.1');
        expect(options.port).to.be.equal('9999');
        expect(options.path).to.be.equal('/eureka/v2/apps/');
        requestStub.restore();
        done();
      });
    });

    it('should call registry URI for delta', (done) => {
      const requestStub = mockSuccessfulResponse();
      client.config.shouldUseDelta = true;
      client.hasFullRegistry = true;
      client.fetchRegistry(() => {
        const options = requestStub.resolvesArg(0).args[0][0];
        expect(options.method).to.be.equal('GET');
        expect(options.hostname).to.be.equal('127.0.0.1');
        expect(options.port).to.be.equal('9999');
        expect(options.path).to.be.equal('/eureka/v2/apps/delta');
        requestStub.restore();
        done();
      });
    });

    it('should throw error for non-200 response', (done) => {
      const requestStub = mockSuccessfulResponse({}, 500);
      client.fetchRegistry((msg) => {
        expect(msg.message).to.be.equal('Unable to retrieve full registry from Eureka server');
        requestStub.restore();
        done();
      });
    });

    it('should throw error for non-200 response for delta', (done) => {
      const requestStub = mockSuccessfulResponse({}, 500);
      client.config.shouldUseDelta = true;
      client.hasFullRegistry = true;
      client.fetchRegistry((msg) => {
        expect(msg.message).to.be.equal('Unable to retrieve delta registry from Eureka server');
        requestStub.restore();
        done();
      });
    });

    it('should throw error for request error', (done) => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').returns({
        on: (type, callback) => { callbacks[type] = callback; },
        write: () => {},
        end: () => {},
      });
      client.fetchRegistry((msg) => {
        expect(msg.message).to.be.equal('request error');
        requestStub.restore();
        done();
      });
      callbacks.error(new Error('request error'));
    });

    it('should throw error for request error for delta request', (done) => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').returns({
        on: (type, callback) => { callbacks[type] = callback; },
        write: () => {},
        end: () => {},
      });
      client.config.shouldUseDelta = true;
      client.hasFullRegistry = true;
      client.fetchRegistry((msg) => {
        expect(msg.message).to.be.equal('request error');
        requestStub.restore();
        done();
      });
      callbacks.error(new Error('request error'));
    });

    it('should throw error on invalid JSON', (done) => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').yields({
        statusCode: 200,
        on: (type, callback) => { callbacks[type] = callback; },
      }).returns({
        on: () => {},
        end: () => {
          callbacks.data('{ blah');
          callbacks.end.apply();
        },
      });

      client.fetchRegistry(error => {
        expect(error instanceof SyntaxError).to.be.true;
        requestStub.restore();
        done();
      });
    });

    it('should throw error on invalid JSON for delta request', (done) => {
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request').yields({
        statusCode: 200,
        on: (type, callback) => { callbacks[type] = callback; },
      }).returns({
        on: () => {},
        end: () => {
          callbacks.data('{ blah');
          callbacks.end.apply();
        },
      });

      client.config.shouldUseDelta = true;
      client.hasFullRegistry = true;
      client.fetchRegistry(error => {
        expect(error instanceof SyntaxError).to.be.true;
        requestStub.restore();
        done();
      });
    });
  });

  describe('transformRegistry()', () => {
    let client;
    let config;
    let registry;
    let instance1;
    let instance2;
    let instance3;
    let instance4;
    let instance5;
    let app1;
    let app2;
    let app3;
    beforeEach(() => {
      config = makeConfig();
      registry = {
        applications: { application: {} },
      };
      instance1 = { hostName: '127.0.0.1', port: { $: 1000 }, app: 'theapp', vipAddress: 'vip1', status: 'UP' };
      instance2 = { hostName: '127.0.0.2', port: { $: 2000 }, app: 'theapptwo', vipAddress: 'vip2', status: 'UP' };
      instance3 = { hostName: '127.0.0.3', port: { $: 2000 }, app: 'theapp', vipAddress: 'vip2', status: 'UP' };
      instance4 = { hostName: '127.0.0.4', port: { $: 2000 }, app: 'theappthree', vipAddress: 'vip3', status: 'UP' };
      instance5 = { hostName: '127.0.0.5', port: { $: 2000 }, app: 'theappthree', vipAddress: 'vip2', status: 'UP' };

      app1 = { name: 'theapp', instance: instance1 };
      app2 = { name: 'theapptwo', instance: [instance2, instance3] };
      app3 = { name: 'theappthree', instance: [instance5, instance4] };
      client = new Eureka(config);
    });

    it('should noop if empty registry', () => {
      client.transformRegistry(undefined);
      expect(client.cache.vip).to.be.empty;
      expect(client.cache.app).to.be.empty;
    });

    it('should return clear the cache if no applications exist', () => {
      registry.applications.application = null;
      client.transformRegistry(registry);
      expect(client.cache.vip).to.be.empty;
      expect(client.cache.app).to.be.empty;
    });

    it('should transform a registry with one app', () => {
      registry.applications.application = app1;
      client.transformRegistry(registry);
      expect(client.cache.app[app1.name.toUpperCase()].length).to.equal(1);
      expect(client.cache.vip[instance1.vipAddress].length).to.equal(1);
    });

    it('should transform a registry with two or more apps', () => {
      registry.applications.application = [app1, app2];
      client.transformRegistry(registry);
      expect(client.cache.app[app1.name.toUpperCase()].length).to.equal(2);
      expect(client.cache.vip[instance2.vipAddress].length).to.equal(2);
    });

    it('should transform a registry with a single application with multiple vips', () => {
      registry.applications.application = [app3];
      client.transformRegistry(registry);
      expect(client.cache.app[app3.name.toUpperCase()].length).to.equal(2);
      expect(client.cache.vip[instance5.vipAddress].length).to.equal(1);
      expect(client.cache.vip[instance4.vipAddress].length).to.equal(1);
    });
  });

  describe('transformApp()', () => {
    let client;
    let config;
    let app;
    let instance1;
    let instance2;
    let instance3;
    let instance4;
    let downInstance;
    let theVip;
    let multiVip;
    let cache;
    beforeEach(() => {
      config = makeConfig({
        instance: { dataCenterInfo: { name: 'Amazon' } },
      });
      client = new Eureka(config);
      theVip = 'theVip';
      multiVip = 'fooVip,barVip';
      instance1 = { hostName: '127.0.0.1', port: 1000, vipAddress: theVip, app: 'theapp', status: 'UP' };
      instance2 = { hostName: '127.0.0.2', port: 2000, vipAddress: theVip, app: 'theapp', status: 'UP' };
      instance3 = { hostName: '127.0.0.5', port: 2000, vipAddress: multiVip, app: 'theapp', status: 'UP' };
      instance4 = { hostName: '127.0.0.6', port: 2000, vipAddress: void 0, app: 'theapp', status: 'UP' };
      downInstance = { hostName: '127.0.0.7', port: 2000, app: 'theapp', vipAddress: theVip, status: 'DOWN' };
      app = { name: 'theapp' };
      cache = { app: {}, vip: {} };
    });

    it('should transform an app with one instance', () => {
      app.instance = instance1;
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(1);
      expect(cache.vip[theVip].length).to.equal(1);
    });

    it('should transform an app with one instance that has a comma separated vipAddress', () => {
      app.instance = instance3;
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(1);
      expect(cache.vip[multiVip.split(',')[0]].length).to.equal(1);
      expect(cache.vip[multiVip.split(',')[1]].length).to.equal(1);
    });

    it('should transform an app with one instance that has no vipAddress', () => {
      app.instance = instance4;
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(1);
      expect(Object.keys(cache.vip).length).to.equal(0);
    });

    it('should transform an app with two or more instances', () => {
      app.instance = [instance1, instance2, instance3];
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(3);
      expect(cache.vip[theVip].length).to.equal(2);
      expect(cache.vip[multiVip.split(',')[0]].length).to.equal(1);
      expect(cache.vip[multiVip.split(',')[1]].length).to.equal(1);
    });

    it('should filter UP instances by default', () => {
      app.instance = [instance1, instance2, downInstance];
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(2);
      expect(cache.vip[theVip].length).to.equal(2);
    });

    it('should not filter UP instances when filterUpInstances === false', () => {
      config = makeConfig({
        instance: { dataCenterInfo: { name: 'Amazon' } },
        eureka: { filterUpInstances: false },
      });
      client = new Eureka(config);
      app.instance = [instance1, instance2, downInstance];
      client.transformApp(app, cache);
      expect(cache.app[app.name.toUpperCase()].length).to.equal(3);
      expect(cache.vip[theVip].length).to.equal(3);
    });
  });

  describe('addInstanceMetadata()', () => {
    let client;
    let config;
    let instanceConfig;
    let awsMetadata;
    let metadataSpy;
    beforeEach(() => {
      instanceConfig = {
        app: 'app',
        vipAddress: '1.2.3.4',
        port: 9999,
        dataCenterInfo: { name: 'Amazon' },
        statusPageUrl: 'http://__HOST__:8080/info',
        healthCheckUrl: 'http://__HOST__:8077/healthcheck',
        homePageUrl: 'http://__HOST__:8080/',
      };
      awsMetadata = {
        'public-hostname': 'ec2-127-0-0-1.us-fake-1.mydomain.com',
        'public-ipv4': '54.54.54.54',
        'local-hostname': 'fake-1',
        'local-ipv4': '10.0.1.1',
      };
    });

    afterEach(() => {
      client.metadataClient.fetchMetadata.restore();
    });

    it('should update hosts with AWS metadata public host', () => {
      // Setup
      config = {
        instance: instanceConfig,
        eureka: { host: '127.0.0.1', port: 9999 },
      };
      client = new Eureka(config);
      metadataSpy = sinon.spy();

      sinon.stub(client.metadataClient, 'fetchMetadata').yields(awsMetadata);

      // Act
      client.addInstanceMetadata(metadataSpy);
      expect(client.config.instance.hostName).to.equal('ec2-127-0-0-1.us-fake-1.mydomain.com');
      expect(client.config.instance.ipAddr).to.equal('54.54.54.54');
      expect(client.config.instance.statusPageUrl).to.equal('http://ec2-127-0-0-1.us-fake-1.mydomain.com:8080/info');
      expect(client.config.instance.healthCheckUrl).to.equal('http://ec2-127-0-0-1.us-fake-1.mydomain.com:8077/healthcheck');
      expect(client.config.instance.homePageUrl).to.equal('http://ec2-127-0-0-1.us-fake-1.mydomain.com:8080/');
    });

    it('should update hosts with AWS metadata public IP when preferIpAddress === true', () => {
      // Setup
      config = {
        instance: instanceConfig,
        eureka: { host: '127.0.0.1', port: 9999, preferIpAddress: true },
      };
      client = new Eureka(config);
      metadataSpy = sinon.spy();

      sinon.stub(client.metadataClient, 'fetchMetadata').yields(awsMetadata);

      // Act
      client.addInstanceMetadata(metadataSpy);
      expect(client.config.instance.hostName).to.equal('54.54.54.54');
      expect(client.config.instance.ipAddr).to.equal('54.54.54.54');
      expect(client.config.instance.statusPageUrl).to.equal('http://54.54.54.54:8080/info');
      expect(client.config.instance.healthCheckUrl).to.equal('http://54.54.54.54:8077/healthcheck');
      expect(client.config.instance.homePageUrl).to.equal('http://54.54.54.54:8080/');
    });

    it('should update hosts with AWS metadata local host if useLocalMetadata === true', () => {
      // Setup
      config = {
        instance: instanceConfig,
        eureka: { host: '127.0.0.1', port: 9999, useLocalMetadata: true },
      };
      client = new Eureka(config);
      metadataSpy = sinon.spy();

      sinon.stub(client.metadataClient, 'fetchMetadata').yields(awsMetadata);

      // Act
      client.addInstanceMetadata(metadataSpy);
      expect(client.config.instance.hostName).to.equal('fake-1');
      expect(client.config.instance.ipAddr).to.equal('10.0.1.1');
      expect(client.config.instance.statusPageUrl).to.equal('http://fake-1:8080/info');
      expect(client.config.instance.healthCheckUrl).to.equal('http://fake-1:8077/healthcheck');
      expect(client.config.instance.homePageUrl).to.equal('http://fake-1:8080/');
    });

    it('should update hosts with AWS metadata local IP if useLocalMetadata === true' +
      ' and preferIpAddress === true', () => {
      // Setup
      config = {
        instance: instanceConfig,
        eureka: { host: '127.0.0.1', port: 9999, useLocalMetadata: true, preferIpAddress: true },
      };
      client = new Eureka(config);
      metadataSpy = sinon.spy();

      sinon.stub(client.metadataClient, 'fetchMetadata').yields(awsMetadata);

      // Act
      client.addInstanceMetadata(metadataSpy);
      expect(client.config.instance.hostName).to.equal('10.0.1.1');
      expect(client.config.instance.ipAddr).to.equal('10.0.1.1');
      expect(client.config.instance.statusPageUrl).to.equal('http://10.0.1.1:8080/info');
      expect(client.config.instance.healthCheckUrl).to.equal('http://10.0.1.1:8077/healthcheck');
      expect(client.config.instance.homePageUrl).to.equal('http://10.0.1.1:8080/');
    });
  });

  describe('eurekaRequest()', () => {
    it('should call requestMiddleware with request options', () => {
      const overrides = {
        requestMiddleware: sinon.spy((opts, done) => done(opts)),
      };
      const requestStub = mockSuccessfulResponse({}, 200);
      const config = makeConfig(overrides);
      const client = new Eureka(config);
      client.eurekaRequest({}, (error) => {
        expect(Boolean(error)).to.equal(false);
        expect(overrides.requestMiddleware).to.be.calledOnce;
        expect(overrides.requestMiddleware.args[0][0]).to.be.an('object');
      });
      requestStub.restore();
    });
    it('should catch an error in requestMiddleware', () => {
      const overrides = {
        requestMiddleware: sinon.spy((opts, done) => {
          done();
        }),
      };
      const requestStub = mockSuccessfulResponse({}, 200);
      const config = makeConfig(overrides);
      const client = new Eureka(config);
      client.eurekaRequest({}, (error) => {
        expect(overrides.requestMiddleware).to.be.calledOnce;
        expect(error).to.be.an('error');
      });
      requestStub.restore();
    });
    it('should check the returnType of requestMiddleware', () => {
      const overrides = {
        requestMiddleware: sinon.spy((opts, done) => done('foo')),
      };
      const requestStub = mockSuccessfulResponse({}, 200);
      const config = makeConfig(overrides);
      const client = new Eureka(config);
      client.eurekaRequest({}, (error) => {
        expect(error).to.be.an('error');
        expect(error.message).to.equal('requestMiddleware did not return an object');
      });
      requestStub.restore();
    });

    it('should retry next server on request failure', (done) => {
      const overrides = {
        eureka: {
          serviceUrls: {
            default: ['http://serverA', 'http://serverB'],
          },
          maxRetries: 3,
          requestRetryDelay: 0,
        },
      };
      const config = makeConfig(overrides);
      const client = new Eureka(config);
      const callbacks = [];
      const requestStub = sinon.stub(https, 'request');
      const req = {
        write: () => {},
        on: (type, callback) => {
          if (!callbacks[type]) callbacks[type] = [];
          callbacks[type].push(callback);
        },
        end: () => {},
      };
      requestStub.yields({
        statusCode: 500,
        on: (type, callback) => {
          callbacks[type] = [callback];
        },
      }).returns(req);

      client.eurekaRequest({ uri: '/path' }, (error) => {
        expect(error).to.be.null;
        expect(requestStub.args[0][0].hostname).to.be.equal('servera');
        expect(requestStub.args[1][0].hostname).to.be.equal('serverb');
        done();
      });

      requestStub.yields({
        statusCode: 200,
        on: (type, callback) => {
          callbacks[type].push(callback);
          req.end = callbacks.end[1];
        },
      }).returns(req);
      callbacks.end[0].apply();
    });
  });


  describe('handleDelta()', () => {
    let client;
    beforeEach(() => {
      const config = makeConfig({ shouldUseDelta: true });
      client = new Eureka(config);
    });

    it('should add instances', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'ADDED' },
          ],
        },
      ];

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(1);
      expect(client.cache.app.THEAPP).to.have.length(1);
    });

    it('should handle duplicate instances on add', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'ADDED' },
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'ADDED' },
          ],
        },
      ];

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(1);
      expect(client.cache.app.THEAPP).to.have.length(1);
    });

    it('should modify instances', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'MODIFIED', newProp: 'foo' },
          ],
        },
      ];
      const original = { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'MODIFIED' };
      client.cache = {
        app: { THEAPP: [original] },
        vip: { thevip: [original] },
      };

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(1);
      expect(client.cache.app.THEAPP).to.have.length(1);
      expect(client.cache.vip.thevip[0]).to.have.property('newProp');
      expect(client.cache.app.THEAPP[0]).to.have.property('newProp');
    });

    it('should modify instances even when status is not UP', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'DOWN', actionType: 'MODIFIED', newProp: 'foo' },
          ],
        },
      ];
      const original = { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'MODIFIED' };
      client.cache = {
        app: { THEAPP: [original] },
        vip: { thevip: [original] },
      };

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(1);
      expect(client.cache.app.THEAPP).to.have.length(1);
      expect(client.cache.vip.thevip[0]).to.have.property('newProp');
      expect(client.cache.app.THEAPP[0]).to.have.property('newProp');
    });

    it('should add if instance doesnt exist when modifying', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'MODIFIED', newProp: 'foo' },
          ],
        },
      ];

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(1);
      expect(client.cache.app.THEAPP).to.have.length(1);
      expect(client.cache.vip.thevip[0]).to.have.property('newProp');
      expect(client.cache.app.THEAPP[0]).to.have.property('newProp');
    });

    it('should delete instances', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'DELETED', newProp: 'foo' },
          ],
        },
      ];
      const original = { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'ADDED' };
      client.cache = {
        app: { THEAPP: [original] },
        vip: { thevip: [original] },
      };

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(0);
      expect(client.cache.app.THEAPP).to.have.length(0);
    });

    it('should not delete instances if they do not exist', () => {
      const appDelta = [
        {
          instance: [
            { hostName: '127.0.0.1', port: { $: 1000 }, app: 'THEAPP', vipAddress: 'thevip', status: 'UP', actionType: 'DELETED', newProp: 'foo' },
          ],
        },
      ];
      client.cache = {
        app: { THEAPP: [] },
        vip: { thevip: [] },
      };

      client.handleDelta(client.cache, appDelta);
      expect(client.cache.vip.thevip).to.have.length(0);
      expect(client.cache.app.THEAPP).to.have.length(0);
    });
  });
});
