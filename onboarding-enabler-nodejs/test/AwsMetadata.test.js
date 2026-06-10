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

import sinon from 'sinon';
import * as chai from 'chai';
import sinonChai from 'sinon-chai';
import AwsMetadata from '../src/AwsMetadata.js';

const expect = chai.expect;

chai.use(sinonChai);

function mockResponses(type) {
  const requestStub = sinon.stub(global, 'fetch');
  const mock = (url, body) => {
    requestStub.withArgs(url).returns(Promise.resolve({
      ok: true,
      statusCode: 200,
      text: () => Promise.resolve(body),
      json: () => Promise.resolve(JSON.parse(body)),
    }));
  };
  const mockError = (url) => {
    requestStub.withArgs(url).returns(Promise.resolve({
      ok: false,
      statusMessage: 'fail',
      statusCode: 500,
      text: () => Promise.resolve(null),
      json: () => Promise.resolve(null),
    }));
  };
  mock('http://127.0.0.1:8888/latest/meta-data/ami-id', 'ami-123');
  mock('http://127.0.0.1:8888/latest/meta-data/instance-id', 'i123');
  mock('http://127.0.0.1:8888/latest/meta-data/instance-type', 'medium');
  mock('http://127.0.0.1:8888/latest/meta-data/local-ipv4', '1.1.1.1');
  mock('http://127.0.0.1:8888/latest/meta-data/local-hostname', 'ip-127-0-0-1');
  mock('http://127.0.0.1:8888/latest/meta-data/placement/availability-zone', 'fake-1');
  switch (type) {
    case 'full':
      mock('http://127.0.0.1:8888/latest/meta-data/public-hostname', 'ec2-127-0-0-1');
      mock('http://127.0.0.1:8888/latest/meta-data/public-ipv4', '2.2.2.2');
      mock('http://127.0.0.1:8888/latest/dynamic/instance-identity/document', '{"accountId":"123456"}');
      break;
    case 'withoutPublic':
      mock('http://127.0.0.1:8888/latest/meta-data/public-hostname', undefined);
      mock('http://127.0.0.1:8888/latest/meta-data/public-ipv4', null);
      mock('http://127.0.0.1:8888/latest/dynamic/instance-identity/document', '{"accountId":"123456"}');
      break;
    case 'failing':
      mockError('http://127.0.0.1:8888/latest/meta-data/public-hostname', undefined);
      mockError('http://127.0.0.1:8888/latest/meta-data/public-ipv4', null);
      mockError('http://127.0.0.1:8888/latest/dynamic/instance-identity/document', '{"accountId":"123456"}');
      break;
    default:
  }
  mock('http://127.0.0.1:8888/latest/meta-data/mac', 'AB:CD:EF:GH:IJ');
  mock('http://127.0.0.1:8888/latest/meta-data/network/interfaces/macs/AB:CD:EF:GH:IJ/vpc-id', 'vpc123');
  return requestStub;
}

describe('AWS Metadata client', () => {
  describe('fetchMetadata()', () => {
    let client;
    beforeEach(() => {
      client = new AwsMetadata({ host: '127.0.0.1:8888' });
    });

    it('should call metadata URIs', (done) => {
      const requestStub = mockResponses('full');
      const expected = {
        accountId: '123456',
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'public-hostname': 'ec2-127-0-0-1',
        'public-ipv4': '2.2.2.2',
        'vpc-id': 'vpc123',
      };

      client.fetchMetadata(data => {
        try {
          expect(data).to.deep.equal(expected);
          done();
        } catch (e) {
          done(e);
        } finally {
          requestStub.restore();
        }
      });
    });

    it('should call metadata URIs and filter out null and undefined values', (done) => {
      const requestStub = mockResponses('withoutPublic');

      const expected = {
        accountId: '123456',
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'vpc-id': 'vpc123',
      };

      client.fetchMetadata(data => {
        try {
          expect(data).to.deep.equal(expected);
          done();
        } catch (e) {
          done(e);
        } finally {
          requestStub.restore();
        }
      });
    });

    it('should call metadata URIs and filter out errored values', (done) => {
      const requestStub = mockResponses('failing');

      const expected = {
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'vpc-id': 'vpc123',
      };

      client.fetchMetadata(data => {
        try {
          expect(data).to.deep.equal(expected);
          done();
        } catch (e) {
          done(e);
        } finally {
          requestStub.restore();
        }
      });
    });
  });
});
