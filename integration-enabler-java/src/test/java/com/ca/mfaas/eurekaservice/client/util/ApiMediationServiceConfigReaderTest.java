/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.util.ObjectUtil;
import com.ca.mfaas.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

public class ApiMediationServiceConfigReaderTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {

        objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setDefaultMergeable(true);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_ABSENT));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();


    @Test
    public void testSetApiMlServiceContext() throws ServiceDefinitionException {
        //ServletContext servletContext = mock(ServletContext.class);

    }

    @Test
    public void testLoadConfiguration() throws ServiceDefinitionException {

        String internalFileName = "/service-configuration.yml";
        String additionalFileName = "/additional-service-configuration.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertNotNull(result);
        assertEquals(result.getServiceId(), "hellopje");
    }


    @Test
    public void testReadConfigurationFile_Existing() throws ServiceDefinitionException {

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        // 1) Existing file
        String internalFileName = "/service-configuration.yml";
        String result = apiMediationServiceConfigReader.readConfigurationFile(internalFileName);
        assertNotNull(result);
        assertNotEquals(result.length(),  -1);
    }

    @Test
    public void readNotExistingConfiguration() throws ServiceDefinitionException {
        String file = "no-existing-file";

        assertNull(new ApiMediationServiceConfigReader().readConfigurationFile(file));
    }

    @Test
    public void readConfigurationWithWrongFormat() throws ServiceDefinitionException {
        String file = "/bad-format-of-service-configuration.yml";
        exceptionRule.expect(ServiceDefinitionException.class);

        ApiMediationServiceConfig  config = new ApiMediationServiceConfigReader().loadConfiguration(file);
        assertNull(config);
    }


    @Test
    public void testMapMerge_FULL() {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = ObjectUtil.mergeMapsDeep(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals(((Map)map3.get("ssl")).get("trustStore"), "../keystore/localhost/localhost.truststore.p12");
        assertEquals(((Map)map3.get("ssl")).get("trustStorePassword"), "password2");
    }

    @Test
    public void testMapMerge_PART_serviceid_keystore_truststore() {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration_serviceid-andkeystore-truststore-only.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = ObjectUtil.mergeMapsDeep(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals((map3.get("serviceId")), "hellozowe");
        assertEquals(((Map)((Map)map3.get("catalog")).get("tile")).get("id"), "hello-zowe");
        assertEquals(((Map)map3.get("ssl")).get("keyStore"), "../keystore/localhost/localhost.keystore.p12");
        assertEquals(((Map)map3.get("ssl")).get("keyStorePassword"), "password1");
        assertEquals(((Map)map3.get("ssl")).get("trustStore"), "../truststore/localhost/localhost.truststore.p12");
        assertEquals(((Map)map3.get("ssl")).get("trustStorePassword"), "password2");
    }


    @Test
    public void testMapMerge_PART_serviceid_ciphers() {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration_serviceid-ssl-ciphers-only.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        Map<String, Object> map3 = ObjectUtil.mergeMapsDeep(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        ApiMediationServiceConfig apiMediationServiceConfig = null;
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals((map3.get("serviceId")), "hellopje");
        assertEquals(((Map)map3.get("ssl")).get("ciphers"), "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
        assertEquals(((Map)map3.get("ssl")).get("keyStore"), "../keystore/localhost/localhost.keystore.p12");
        assertEquals(((Map)map3.get("ssl")).get("keyStorePassword"), "password");
        assertEquals(((Map)map3.get("ssl")).get("trustStore"), "../keystore/localhost/localhost.truststore.p12");
        assertEquals(((Map)map3.get("ssl")).get("trustStorePassword"), "password");
    }


    @Test
    public void testGetApiMediationServiceConfigFromFile() {
        Map<String, String> properties = new HashMap<>();
        ApiMediationServiceConfig config = getApiMediationServiceConfigFromFile("/service-configuration.yml", properties);

        assertNotNull(config);
    }

    private ApiMediationServiceConfig getApiMediationServiceConfigFromFile(String fileName, Map<String, String> properties) {

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig apimlServcieConfig1 = null;
        try {
            String result = apiMediationServiceConfigReader.readConfigurationFile(fileName);
            result = StringUtils.resolveExpressions(result, properties);
            apimlServcieConfig1 = apiMediationServiceConfigReader.buildConfiguration(result);
        } catch (ServiceDefinitionException e) {
            System.err.print("ServiceDefinitionException caught :");
            e.printStackTrace();
        }

        return apimlServcieConfig1;
    }
}
