/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.acceptance.config.ApimlRoutingConfig;
import org.zowe.apiml.acceptance.config.DiscoveryClientTestConfig;
import org.zowe.apiml.acceptance.config.GatewayOverrideConfig;
import org.zowe.apiml.acceptance.config.GatewayTestApplication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = GatewayTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({GatewayOverrideConfig.class, DiscoveryClientTestConfig.class, ApimlRoutingConfig.class})
public @interface AcceptanceTest {
}
