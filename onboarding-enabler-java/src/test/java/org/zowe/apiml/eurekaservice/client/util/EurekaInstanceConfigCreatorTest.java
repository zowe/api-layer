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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EurekaInstanceConfigCreatorTest {

    private ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator();

    @Test
    public void givenNullMetadata_whenConverted_shouldReturnEmptyMap() {
        Map<String, Object> testMap = new HashMap<>();
        Map<String, String> resultMap = eurekaInstanceConfigCreator.flattenMetadata(testMap);
        assertTrue(resultMap.isEmpty());
    }

    @Test
    public void givenMetadataWithNullValue_whenConverted_shouldReturnEmptyValue() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", null);
        Map<String, String> resultMap = eurekaInstanceConfigCreator.flattenMetadata(testMap);
        assertThat(resultMap, hasEntry("key", ""));
    }

    @Test
    public void givenMetadataWithNestedMap_whenConverted_shouldReturnFlattened() {
        Map<String, Object> nestedLvl2 = new HashMap<>();
        nestedLvl2.put("keyzzz", "valuezzz");

        Map<String, Object> nested = new HashMap<>();
        nested.put("key1", "value1");
        nested.put("key2", "value2");
        nested.put("key3", nestedLvl2);

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        Map<String, String> resultMap = eurekaInstanceConfigCreator.flattenMetadata(testMap);
        assertThat(resultMap, hasEntry("masterKey.key1", "value1"));
        assertThat(resultMap, hasEntry("masterKey.key2", "value2"));
        assertThat(resultMap, hasEntry("masterKey.key3.keyzzz", "valuezzz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenMetadataWithNestedList_whenConverted_shouldReturnException() {
        List<Object> nested = new ArrayList<>();
        nested.add("value1");
        nested.add("value2");

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        Map<String, String> resultMap = eurekaInstanceConfigCreator.flattenMetadata(testMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenMetadataWithNestedArray_whenConverted_shouldReturnException() {
        String[] nested = {"value1", "value2"};

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        Map<String, String> resultMap = eurekaInstanceConfigCreator.flattenMetadata(testMap);
    }


    @Test
    public void givenYamlMetadata_whenParsedByJackson_shouldFlattenMetadataCorrect() throws ServiceDefinitionException {
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
