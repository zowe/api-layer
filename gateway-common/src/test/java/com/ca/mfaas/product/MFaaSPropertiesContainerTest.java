/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "eureka.client.fetchRegistry=false",
        "eureka.client.registerWithEureka=false"
    },
    classes = {TestConfiguration.class})
@ContextConfiguration(classes = TestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class MFaaSPropertiesContainerTest {

    @Autowired
    private MFaaSConfigPropertiesContainer configPropertiesContainer;

    @Test
    public void testAllSettingsNotNull() {
        assertNotNull(configPropertiesContainer);

        assertNotNull(configPropertiesContainer.getCatalogUiTile());
        assertNotNull(configPropertiesContainer.getCatalogUiTile().getDescription());
        assertNotNull(configPropertiesContainer.getCatalogUiTile().getId());
        assertNotNull(configPropertiesContainer.getCatalogUiTile().getTitle());
        assertNotNull(configPropertiesContainer.getCatalogUiTile().getVersion());

        assertNotNull(configPropertiesContainer.getSecurity());
        assertNotNull(configPropertiesContainer.getSecurity().getSslEnabled());
        assertNotNull(configPropertiesContainer.getSecurity().getCiphers());
        assertNotNull(configPropertiesContainer.getSecurity().getEsmEnabled());
        assertNotNull(configPropertiesContainer.getSecurity().getProtocol());
        assertNotNull(configPropertiesContainer.getServer().getScheme());
        assertNotNull(configPropertiesContainer.getSecurity().getKeyStore());
        assertNotNull(configPropertiesContainer.getSecurity().getKeyAlias());
        assertNotNull(configPropertiesContainer.getSecurity().getKeyPassword());
        assertNotNull(configPropertiesContainer.getSecurity().getKeyStoreType());
        assertNotNull(configPropertiesContainer.getSecurity().getKeyStorePassword());
        assertNotNull(configPropertiesContainer.getSecurity().getTrustStore());
        assertNotNull(configPropertiesContainer.getSecurity().getTrustStorePassword());
        assertNotNull(configPropertiesContainer.getSecurity().getTrustStoreType());

        assertNotNull(configPropertiesContainer.getService());
        assertNotNull(configPropertiesContainer.getService().getHostname());
        assertNotNull(configPropertiesContainer.getService().getIpAddress());

        assertNotNull(configPropertiesContainer.getDiscovery());
        assertNotNull(configPropertiesContainer.getDiscovery().getEnabled());
        assertNotNull(configPropertiesContainer.getDiscovery().getServiceId());
        assertNotNull(configPropertiesContainer.getDiscovery().getLocations());
        assertNotNull(configPropertiesContainer.getDiscovery().getInfo());
        assertNotNull(configPropertiesContainer.getDiscovery().getInfo().getDescription());
        assertNotNull(configPropertiesContainer.getDiscovery().getInfo().getEnableApiDoc());
        assertNotNull(configPropertiesContainer.getDiscovery().getInfo().getServiceTitle());
        assertNotNull(configPropertiesContainer.getDiscovery().getEndpoints());
        assertNotNull(configPropertiesContainer.getDiscovery().getEndpoints().getHealthPage());
        assertNotNull(configPropertiesContainer.getDiscovery().getEndpoints().getHomepage());
        assertNotNull(configPropertiesContainer.getDiscovery().getEndpoints().getStatusPage());

        assertNotNull(configPropertiesContainer.getServer());
        assertNotNull(configPropertiesContainer.getServer().getIpAddress());
        assertNotNull(configPropertiesContainer.getServer().getContextPath());
        assertNotNull(configPropertiesContainer.getServer().getPort());
        assertNotNull(configPropertiesContainer.getServer().getSecurePort());
        assertNotNull(configPropertiesContainer.getServer().getPreferIpAddress());

        assertNotNull(configPropertiesContainer.getGateway());
        assertNotNull(configPropertiesContainer.getGateway().getTimeoutInMillis());
        assertNotNull(configPropertiesContainer.getGateway().getDebugHeaders());
        assertNotNull(configPropertiesContainer.getGateway().getGatewayHostname());

        assertNotNull(configPropertiesContainer.getServiceRegistry());
        assertNotNull(configPropertiesContainer.getServiceRegistry().getServiceFetchDelayInMillis());
        assertNotNull(configPropertiesContainer.getServiceRegistry().getCacheRefreshInitialDelayInMillis());
        assertNotNull(configPropertiesContainer.getServiceRegistry().getCacheRefreshRetryDelayInMillis());
        assertNotNull(configPropertiesContainer.getServiceRegistry().getCacheRefreshUpdateThresholdInMillis());
    }
}
