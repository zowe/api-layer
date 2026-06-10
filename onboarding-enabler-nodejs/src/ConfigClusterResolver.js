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

import Logger from './Logger.js';

/*
  Locates a Eureka host using static configuration. Configuration can either be
  done using a simple host and port, or a map of serviceUrls.
 */
export default class ConfigClusterResolver {
  constructor(config, logger) {
    this.logger = logger || new Logger();
    this.config = config;
    this.serviceUrls = this.buildServiceUrls();
  }

  resolveEurekaUrl(callback, retryAttempt = 0) {
    if (this.serviceUrls.length > 1 && retryAttempt > 0) {
      this.serviceUrls.push(this.serviceUrls.shift());
    }
    callback(null, this.serviceUrls[0]);
  }

  buildServiceUrls() {
    const { host, port, servicePath, ssl,
      serviceUrls, preferSameZone } = this.config.eureka;
    const { dataCenterInfo } = this.config.instance;
    const metadata = dataCenterInfo ? dataCenterInfo.metadata : undefined;
    const instanceZone = metadata ? metadata['availability-zone'] : undefined;
    const urls = [];
    const zones = this.getAvailabilityZones();
    if (serviceUrls) {
      zones.forEach((zone) => {
        if (serviceUrls[zone]) {
          if (preferSameZone && instanceZone && instanceZone === zone) {
            urls.unshift(...serviceUrls[zone]);
          }
          urls.push(...serviceUrls[zone]);
        }
      });
    }
    if (!urls.length) {
      const protocol = ssl ? 'https' : 'http';
      urls.push(`${protocol}://${host}:${port}${servicePath}`);
    }
    return urls;
  }

  getAvailabilityZones() {
    const { ec2Region, availabilityZones } = this.config.eureka;
    if (ec2Region && availabilityZones && availabilityZones[ec2Region]) {
      return availabilityZones[ec2Region];
    }
    return ['default'];
  }
}
