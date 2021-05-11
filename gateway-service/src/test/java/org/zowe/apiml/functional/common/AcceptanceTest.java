/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.functional.config.ApimlRoutingConfig;
import org.zowe.apiml.functional.config.DiscoveryClientTestConfig;
import org.zowe.apiml.functional.config.GatewayOverrideConfig;
import org.zowe.apiml.functional.config.GatewayTestApplication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = GatewayTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.server.port=10090","server.internal.enabled=false"})
@Import({GatewayOverrideConfig.class, DiscoveryClientTestConfig.class, ApimlRoutingConfig.class})
public @interface AcceptanceTest {
}
