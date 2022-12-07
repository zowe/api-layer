/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.zowe.apiml.cloudgatewayservice.acceptance.config.CloudGatewayServiceTestApplication;
import org.zowe.apiml.cloudgatewayservice.acceptance.config.DiscoveryClientTestConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan(basePackages = "org.zowe.apiml.cloudgatewayservice")
@SpringBootTest(classes = CloudGatewayServiceTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.server.port=10091","server.internal.enabled=false"})
@Import(DiscoveryClientTestConfig.class)
@DirtiesContext
public @interface AcceptanceTest {
}
