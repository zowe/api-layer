/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.util;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.registry.model.ServiceInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EurekaInstanceConfigCreatorTest {

    private ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator();

    @Test
    void givenYamlMetadata_whenParsedByJackson_shouldFlattenMetadataCorrectly() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration.yml");
        ServiceInstance translatedConfig = eurekaInstanceConfigCreator.createServiceInstance(testConfig);

        assertThat(translatedConfig.metadata(), hasEntry("key", "value"));
        assertThat(translatedConfig.metadata(), hasEntry("customService.key1", "value1"));
        assertThat(translatedConfig.metadata(), hasEntry("customService.key2", "value2"));
        assertThat(translatedConfig.metadata(), hasEntry("customService.key3", "value3"));
        assertThat(translatedConfig.metadata(), hasEntry("customService.key4", "value4"));
        assertThat(translatedConfig.metadata(), hasEntry("customService.evenmorelevels.key5.key6.key7", "value7"));
    }

    @Test
    void givenYamlMetadata_whenIpAddressIsPreferred_thenUseIpAddress() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration-prefer-ip.yml");
        ServiceInstance translatedConfig = eurekaInstanceConfigCreator.createServiceInstance(testConfig);
        assertEquals("http://127.0.0.1:10021/", translatedConfig.homePageUrl());
    }

    @Test
    void givenConfigurationWithInvalidProtocol_whenValidate_thenThrowException() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("bad-protocol-baseurl-service-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> eurekaInstanceConfigCreator.createServiceInstance(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("'ftp' is not valid protocol for baseUrl property", exception.getMessage());
    }

}
