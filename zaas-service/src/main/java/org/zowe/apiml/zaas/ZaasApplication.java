/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.zowe.apiml.extension.ExtensionConfigReader;
import org.zowe.apiml.extension.ExtensionsLoader;
import org.zowe.apiml.product.monitoring.LatencyUtilsConfigInitializer;
import org.zowe.apiml.product.version.BuildInfo;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

import static org.zowe.apiml.extension.ZoweRuntimeEnvironment.defaultEnv;

@EnableWebSecurity
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(
    value = {
        "org.zowe.apiml.zaas",
        "org.zowe.apiml.product",
        "org.zowe.apiml.security.common"
    }
)
@EnableConfigurationProperties(SafSecurityConfigurationProperties.class)
public class ZaasApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ZaasApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.addListeners(new ExtensionsLoader(new ExtensionConfigReader(defaultEnv())));
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

}
