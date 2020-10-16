/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.zowe.apiml.enable.EnableApiDiscovery;

@SpringBootApplication
@EnableApiDiscovery
public class CachingService {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CachingService.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }
}
