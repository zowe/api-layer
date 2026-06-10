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

import dns from 'dns';
import async from 'async';
import shuffle from 'lodash/shuffle.js';
import xor from 'lodash/xor.js';
import Logger from './Logger.js';

function noop() {}

/*
  Locates a Eureka host using DNS lookups. The DNS records are looked up by a naming
  convention and TXT records must be created according to the Eureka Wiki here:
  https://github.com/Netflix/eureka/wiki/Configuring-Eureka-in-AWS-Cloud

  Naming convention: txt.<REGION>.<HOST>
 */
export default class DnsClusterResolver {
  constructor(config, logger) {
    this.logger = logger || new Logger();
    this.serverList = undefined;
    this.config = config;
    if (!this.config.eureka.ec2Region) {
      throw new Error(
        'EC2 region was undefined. ' +
        'config.eureka.ec2Region must be set to resolve Eureka using DNS records.'
      );
    }

    if (this.config.eureka.clusterRefreshInterval) {
      this.startClusterRefresh();
    }
  }

  resolveEurekaUrl(callback, retryAttempt = 0) {
    this.getCurrentCluster((err) => {
      if (err) return callback(err);

      if (retryAttempt > 0) {
        this.serverList.push(this.serverList.shift());
      }
      const { port, servicePath, ssl } = this.config.eureka;
      const protocol = ssl ? 'https' : 'http';
      callback(null, `${protocol}://${this.serverList[0]}:${port}${servicePath}`);
    });
  }

  getCurrentCluster(callback) {
    if (this.serverList) {
      return callback(null, this.serverList);
    }
    this.refreshCurrentCluster((err) => {
      if (err) return callback(err);
      return callback(null, this.serverList);
    });
  }

  startClusterRefresh() {
    const refreshTimer = setInterval(() => {
      this.refreshCurrentCluster((err) => {
        if (err) this.logger.warn(err.message);
      });
    }, this.config.eureka.clusterRefreshInterval);
    refreshTimer.unref();
  }

  refreshCurrentCluster(callback = noop) {
    this.resolveClusterHosts((err, hosts) => {
      if (err) return callback(err);
      // if the cluster is the same (aside from order), we want to maintain our order
      if (xor(this.serverList, hosts).length) {
        this.serverList = hosts;
        this.logger.info('Eureka cluster located, hosts will be used in the following order',
          this.serverList);
      } else {
        this.logger.debug('Eureka cluster hosts unchanged, maintaining current server list.');
      }
      callback();
    });
  }

  resolveClusterHosts(callback = noop) {
    const { ec2Region, host, preferSameZone } = this.config.eureka;
    const { dataCenterInfo } = this.config.instance;
    const metadata = dataCenterInfo ? dataCenterInfo.metadata : undefined;
    const availabilityZone = metadata ? metadata['availability-zone'] : undefined;
    const dnsHost = `txt.${ec2Region}.${host}`;
    dns.resolveTxt(dnsHost, (err, addresses) => {
      if (err) {
        return callback(new Error(
          `Error resolving eureka cluster for region [${ec2Region}] using DNS: [${err}]`
        ));
      }
      const zoneRecords = [].concat(...addresses);
      const dnsTasks = {};
      zoneRecords.forEach((zoneRecord) => {
        dnsTasks[zoneRecord] = (cb) => {
          this.resolveZoneHosts(`txt.${zoneRecord}`, cb);
        };
      });
      async.parallel(dnsTasks, (error, results) => {
        if (error) return callback(error);
        const hosts = [];
        const myZoneHosts = [];
        Object.keys(results).forEach((zone) => {
          if (preferSameZone && availabilityZone && zone.lastIndexOf(availabilityZone, 0) === 0) {
            myZoneHosts.push(...results[zone]);
          } else {
            hosts.push(...results[zone]);
          }
        });
        const combinedHosts = [].concat(shuffle(myZoneHosts), shuffle(hosts));
        if (!combinedHosts.length) {
          return callback(
            new Error(`Unable to locate any Eureka hosts in any zone via DNS @ ${dnsHost}`));
        }
        callback(null, combinedHosts);
      });
    });
  }

  resolveZoneHosts(zoneRecord, callback) {
    dns.resolveTxt(zoneRecord, (err, results) => {
      if (err) {
        this.logger.warn(`Failed to resolve cluster zone ${zoneRecord}`, err.message);
        return callback(new Error(`Error resolving cluster zone ${zoneRecord}: [${err}]`));
      }
      this.logger.debug(`Found Eureka Servers @ ${zoneRecord}`, results);
      callback(null, ([].concat(...results)).filter((value) => (!!value)));
    });
  }
}
