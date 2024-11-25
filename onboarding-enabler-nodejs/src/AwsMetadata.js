/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Copyright Contributors to the Zowe Project.
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
      const error = response.error();
      if (error) {
        this.logger.error('Error requesting metadata key', error);
      }
      callback(null, (error || response.statusCode !== 200) ? null : response.body());
    });
  }

  lookupInstanceIdentity(callback) {
    fetch(`http://${this.host}/latest/dynamic/instance-identity/document`).then(response => {
      const error = response.error();
      if (error) {
        this.logger.error('Error requesting instance identity document', error);
      }
      callback(null, (error || response.statusCode !== 200) ? null : JSON.parse(response.body()));
    });
  }
}
