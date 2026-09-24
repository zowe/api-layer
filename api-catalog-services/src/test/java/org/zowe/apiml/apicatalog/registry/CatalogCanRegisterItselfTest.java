/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.zowe.apiml.apicatalog.ApiCatalogApplication;
import org.zowe.apiml.registry.client.spring.RegistryClientLifecycle;
import org.zowe.apiml.util.config.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Catalog registers itself with the Discovery Service and looks other services up through Spring Cloud's
 * DiscoveryClient. Both used to arrive with the Eureka starter. When that starter was dropped, the Catalog was left
 * with neither, and nothing in the build noticed - it still compiled, still started, and still declared its
 * {@code eureka.instance.*} registration settings, it just never appeared in the registry.
 *
 * <p>That is invisible in a local run, where the Catalog is picked up from the static definitions, and fatal in the
 * containerised integration tests, where the static definitions do not load. There the startup check waits eight
 * minutes for {@code api-catalog-services:apicatalog:10014} to onboard, never sees it, and every integration test job
 * skips its tests. Asserting the beans here is what makes the missing dependency a build failure instead.
 */
@SpringBootTest(
    classes = ApiCatalogApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration
@Import(TestConfig.class)
@ActiveProfiles("test")
class CatalogCanRegisterItselfTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("the Catalog has a client that can register it and look services up")
    void catalogHasARegistryClient() {
        assertThat(context.getBean(RegistryClientLifecycle.class))
            .as("without a lifecycle nothing registers the Catalog with the Discovery Service")
            .isNotNull();

        assertThat(context.getBean(DiscoveryClient.class))
            .as("without a discovery client the Catalog cannot find the services it documents")
            .isNotNull();
    }
}
