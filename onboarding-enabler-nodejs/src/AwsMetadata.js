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

import async from 'async';
import Logger from './Logger.js';

/*
  Utility class for pulling AWS metadata that Eureka requires when
  registering as an Amazon instance (datacenter).
*/
export default class AwsMetadata {
  constructor(config = {}) {
    this.logger = config.logger || new Logger();
    this.host = config.host || '169.254.169.254';
  }

  fetchMetadata(resultsCallback) {
    async.parallel({
      'ami-id': callback => {
        this.lookupMetadataKey('ami-id', callback);
      },
      'instance-id': callback => {
        this.lookupMetadataKey('instance-id', callback);
      },
      'instance-type': callback => {
        this.lookupMetadataKey('instance-type', callback);
      },
      'local-ipv4': callback => {
        this.lookupMetadataKey('local-ipv4', callback);
      },
      'local-hostname': callback => {
        this.lookupMetadataKey('local-hostname', callback);
      },
      'availability-zone': callback => {
        this.lookupMetadataKey('placement/availability-zone', callback);
      },
      'public-hostname': callback => {
        this.lookupMetadataKey('public-hostname', callback);
      },
      'public-ipv4': callback => {
        this.lookupMetadataKey('public-ipv4', callback);
      },
      mac: callback => {
        this.lookupMetadataKey('mac', callback);
      },
      accountId: callback => {
        // the accountId is in the identity document.
        this.lookupInstanceIdentity((error, identity) => {
          callback(null, identity ? identity.accountId : null);
        });
      },
    }, (error, results) => {
      // we need the mac before we can lookup the vpcId...
      this.lookupMetadataKey(`network/interfaces/macs/${results.mac}/vpc-id`, (err, vpcId) => {
        results['vpc-id'] = vpcId;
        this.logger.debug('Found Instance AWS Metadata', results);
        const filteredResults = Object.keys(results).reduce((filtered, prop) => {
          if (results[prop]) filtered[prop] = results[prop];
          return filtered;
        }, {});
        resultsCallback(filteredResults);
      });
    });
  }

  lookupMetadataKey(key, callback) {
    fetch(`http://${this.host}/latest/meta-data/${key}`).then(response => {
      let error = null;
      if (!response.ok) {
        error = new Error(`${response.statusCode}: ${response.statusMessage}`);
        this.logger.error('Error requesting metadata key', error);
      }
      response.text().then(text => {
        callback(null, (error || response.statusCode !== 200) ? null : text);
      });
    });
  }

  lookupInstanceIdentity(callback) {
    fetch(`http://${this.host}/latest/dynamic/instance-identity/document`).then(response => {
      let error = null;
      if (!response.ok) {
        error = new Error(`${response.statusCode}: ${response.statusMessage}`);
        this.logger.error('Error requesting instance identity document', error);
      }
      response.json().then(json => {
        callback(null, (error || response.statusCode !== 200) ? null : json);
      });
    });
  }
}
