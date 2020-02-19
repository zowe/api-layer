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

import com.netflix.appinfo.EurekaInstanceConfig;
import org.junit.Test;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class EurekaInstanceConfigCreatorTest {

    private ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator();

    @Test
    public void givenYamlMetadata_whenParsedByJackson_shouldFlattenMetadataCorrectly() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration.yml");
        EurekaInstanceConfig translatedConfig = eurekaInstanceConfigCreator.createEurekaInstanceConfig(testConfig);

        assertThat(translatedConfig.getMetadataMap(), hasEntry("key", "value"));
        assertThat(translatedConfig.getMetadataMap(), hasEntry("customService.key1", "value1"));
        assertThat(translatedConfig.getMetadataMap(), hasEntry("customService.key2", "value2"));
        assertThat(translatedConfig.getMetadataMap(), hasEntry("customService.key3", "value3"));
        assertThat(translatedConfig.getMetadataMap(), hasEntry("customService.key4", "value4"));
        assertThat(translatedConfig.getMetadataMap(), hasEntry("customService.evenmorelevels.key5.key6.key7", "value7"));
    }


}
