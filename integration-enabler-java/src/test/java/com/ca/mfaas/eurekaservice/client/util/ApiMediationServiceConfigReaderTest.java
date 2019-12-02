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
import com.ca.mfaas.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContext;
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
    public void testLoadConfiguration_TwoFiles_OK() throws ServiceDefinitionException {

        String internalFileName = "/service-configuration.yml";
        String additionalFileName = "/additional-service-configuration.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertNotNull(result);
        assertEquals(result.getServiceId(), "hellopje");

        // Use default internal file name
        result = apiMediationServiceConfigReader.loadConfiguration(null, additionalFileName);

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
    public void testMapMerge_FULL() throws ServiceDefinitionException {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = new ApiMediationServiceConfigReader().mergeConfigurations(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals("../keystore/localhost/localhost.truststore.p12", ((Map)map3.get("ssl")).get("trustStore"));
        assertEquals("password2", ((Map)map3.get("ssl")).get("trustStorePassword"));
    }

    @Test
    public void testMapMerge_Only_Externalized() throws ServiceDefinitionException {
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = null;
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = new ApiMediationServiceConfigReader().mergeConfigurations(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals("../keystore/localhost/localhost.truststore.p12", ((Map)map3.get("ssl")).get("trustStore"));
        assertEquals("password2", ((Map)map3.get("ssl")).get("trustStorePassword"));
    }

    @Test
    public void testMapMerge_PART_serviceid_keystore_truststore() throws ServiceDefinitionException {

        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration_serviceid-andkeystore-truststore-only.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = new ApiMediationServiceConfigReader().mergeConfigurations(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals("hellozowe", (map3.get("serviceId")));
        assertEquals("hello-zowe", ((Map)((Map)map3.get("catalog")).get("tile")).get("id"));
        assertEquals("../keystore/localhost/localhost.keystore.p12", ((Map)map3.get("ssl")).get("keyStore"));
        assertEquals("password1", ((Map)map3.get("ssl")).get("keyStorePassword"));
        assertEquals("../truststore/localhost/localhost.truststore.p12", ((Map)map3.get("ssl")).get("trustStore"));
        assertEquals("password2", ((Map)map3.get("ssl")).get("trustStorePassword"));
    }


    @Test
    public void testMapMerge_PART_serviceid_ciphers() throws ServiceDefinitionException {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile( "/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile( "/additional-service-configuration_serviceid-ssl-ciphers-only.yml", null);

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        Map<String, Object> map3 = new ApiMediationServiceConfigReader().mergeConfigurations(defaultConfigPropertiesMap, additionalConfigPropertiesMap);
        ApiMediationServiceConfig apiMediationServiceConfig = null;
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }

        assertNotNull(apiMediationServiceConfig);
        assertEquals("hellopje", (map3.get("serviceId")));
        assertEquals("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", ((Map)map3.get("ssl")).get("ciphers"));
        assertEquals("../keystore/localhost/localhost.keystore.p12", ((Map)map3.get("ssl")).get("keyStore"));
        assertEquals("password", ((Map)map3.get("ssl")).get("keyStorePassword"));
        assertEquals("../keystore/localhost/localhost.truststore.p12", ((Map)map3.get("ssl")).get("trustStore"));
        assertEquals("password", ((Map)map3.get("ssl")).get("trustStorePassword"));
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


    @Test
    public void testSetApiMlContextMap_Ok() {
        Map<String, String> aMap = getMockServletContextMap();
        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        Map<String, String>  contextMap = reader.setApiMlServiceContext(aMap);
        checkContextMap(contextMap);
    }

    @Test
    public void testSetApiMlContext_Ok() {
        ServletContext context = getMockServletContext();

        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        Map<String, String>  contextMap = reader.setApiMlServiceContext(context);
        //reader.
        checkContextMap(contextMap);
    }

    private void checkContextMap(Map<String, String> contextMap) {
        assertNotNull(contextMap);
        assertNull(contextMap.get("NOT-AN-apiml.config.location"));
        assertEquals("/service-config.yml", contextMap.get("apiml.config.location"));
        assertEquals("../config/local/helloworld-additional-config.yml", contextMap.get("apiml.config.additional-location"));
        assertEquals("127.0.0.2", contextMap.get("apiml.serviceIpAddress"));
        assertEquals("10011", contextMap.get("apiml.discoveryService.port"));
        assertEquals("localhost", contextMap.get("apiml.discoveryService.hostname"));
        assertEquals("true", contextMap.get("apiml.ssl.enabled"));
        assertEquals("true", contextMap.get("apiml.ssl.verifySslCertificatesOfServices"));
        assertEquals("password", contextMap.get("apiml.ssl.keyPassword"));
        assertEquals("password", contextMap.get("apiml.ssl.keyStorePassword"));
        assertEquals("password", contextMap.get("apiml.ssl.trustStorePassword"));
    }

    @Test
    public void testSetApiMlContextTwice_Ok() {
        ServletContext context = getMockServletContext();

        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        Map<String, String>  contextMap = reader.setApiMlServiceContext(context);

        contextMap = reader.setApiMlServiceContext(context);

        checkContextMap(contextMap);
    }

    private Map<String, String>getMockServletContextMap() {
        ServletContext context = getMockServletContext();
        Map<String, String> aMap = contextToMap(context);
        return aMap;
    }

    private Map<String, String> contextToMap(ServletContext servletContext) {
        Map<String, String> contextMap = new HashMap<>();
        Enumeration<String> paramNames = servletContext.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String value = servletContext.getInitParameter(param);
            if (param.startsWith("apiml.")) {
                contextMap.put(param, value);
            }
        }
        return contextMap;
    }

    private ServletContext getMockServletContext() {
        ServletContext context = new MockServletContext();
        context.setInitParameter("NOT-AN-apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.additional-location", "../config/local/helloworld-additional-config.yml");
        context.setInitParameter("apiml.serviceIpAddress", "127.0.0.2");
        context.setInitParameter("apiml.discoveryService.port", "10011");
        context.setInitParameter("apiml.discoveryService.hostname", "localhost");
        context.setInitParameter("apiml.ssl.enabled", "true");
        context.setInitParameter("apiml.ssl.verifySslCertificatesOfServices", "true");
        context.setInitParameter("apiml.ssl.keyPassword", "password");
        context.setInitParameter("apiml.ssl.keyStorePassword", "password");
        context.setInitParameter("apiml.ssl.trustStorePassword", "password");
        return context;
    }


    @Test
    public void testReadConfigurationFile_Internal_file_name_null() throws ServiceDefinitionException {

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        // 1) Existing file
        String internalFileName = "/service-configuration.yml";
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(null, internalFileName);
        assertNotNull(result);
    }

    @Test
    public void testLoadConfiguration_IpAddressIsNull_OK() throws ServiceDefinitionException {
        String internalFileName = "/additional-service-configuration_ip-address-null.yml";
        String additionalFileName = "/additional-service-configuration_ip-address-null.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertNotNull(result);
        assertEquals(result.getServiceId(), "service");
    }
}
