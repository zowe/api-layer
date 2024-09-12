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
import org.springframework.mock.web.MockServletContext;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import jakarta.servlet.ServletContext;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiMediationServiceConfigReaderTest {

    @Test
    void testLoadConfiguration_TwoFiles_OK() throws ServiceDefinitionException {

        String internalFileName = "/service-configuration.yml";
        String additionalFileName = "/additional-service-configuration.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertNotNull(result);
        assertEquals("hellopje", result.getServiceId());

        //The metadata deserializes into map, so it creates this structure. It is expected, it is flattened later by ApiMediationClient before sending to Eureka
        assertEquals("{key=anotherValue, customService.key1=value1, customService.key2=value2, customService={key3=value3, key4=value4, evenmorelevels={key5={key6={key7=anothervalue7}}}, key10=value10}}", result.getCustomMetadata().toString());

        // Use default internal file name
        result = apiMediationServiceConfigReader.loadConfiguration(null, additionalFileName);

        assertNotNull(result);
        assertEquals("hellopje", result.getServiceId());
    }

    @Test
    void whenIPAddressMissing_thenProvideLocalhostAsDefault() throws ServiceDefinitionException {
        String file = "/service-configuration-without-ip-address.yml";

        ApiMediationServiceConfigReader apimlConfigReader = new ApiMediationServiceConfigReader();
        assertEquals("127.0.0.1", apimlConfigReader.loadConfiguration(file).getServiceIpAddress());
    }

    @Test
    void testMapMerge_FULL() throws ServiceDefinitionException {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile("/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile("/additional-service-configuration.yml", null);

        ApiMediationServiceConfig apiMediationServiceConfig = new ApiMediationServiceConfigReader().mergeConfigurations(apimlServcieConfig1, apimlServcieConfig2);

        assertNotNull(apiMediationServiceConfig);
        assertEquals("../keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());
        assertArrayEquals("password2".toCharArray(), apiMediationServiceConfig.getSsl().getTrustStorePassword());

        apiMediationServiceConfig = new ApiMediationServiceConfigReader().mergeConfigurations(apimlServcieConfig1, null);
        assertNotNull(apiMediationServiceConfig);
        assertEquals(apiMediationServiceConfig, apimlServcieConfig1);
        assertEquals("keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());
        assertArrayEquals("password".toCharArray(), apiMediationServiceConfig.getSsl().getTrustStorePassword());

        apiMediationServiceConfig = new ApiMediationServiceConfigReader().mergeConfigurations(null, apimlServcieConfig2);
        assertNotNull(apiMediationServiceConfig);
        assertEquals(apiMediationServiceConfig, apimlServcieConfig2);
        assertEquals("../keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());
        assertArrayEquals("password2".toCharArray(), apiMediationServiceConfig.getSsl().getTrustStorePassword());
    }

    @Test
    void testMapMerge_PART_serviceid_keystore_truststore() throws ServiceDefinitionException {

        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile("/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile("/additional-service-configuration_serviceid-andkeystore-truststore-only.yml", null);

        ApiMediationServiceConfig apiMediationServiceConfig = new ApiMediationServiceConfigReader().mergeConfigurations(apimlServcieConfig1, apimlServcieConfig2);

        assertNotNull(apiMediationServiceConfig);
        assertEquals("hellozowe", apiMediationServiceConfig.getServiceId());
        assertEquals("hello-zowe", apiMediationServiceConfig.getCatalog().getTile().getId());
        assertEquals("../keystore/localhost/localhost.keystore.p12", apiMediationServiceConfig.getSsl().getKeyStore());
        assertArrayEquals("password1".toCharArray(), apiMediationServiceConfig.getSsl().getKeyStorePassword());
        assertEquals("../truststore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());
        assertArrayEquals("password2".toCharArray(), apiMediationServiceConfig.getSsl().getTrustStorePassword());
    }

    @Test
    void testMapMerge_PART_serviceid_ciphers() throws ServiceDefinitionException {
        ApiMediationServiceConfig apimlServcieConfig1 = getApiMediationServiceConfigFromFile("/service-configuration.yml", null);
        ApiMediationServiceConfig apimlServcieConfig2 = getApiMediationServiceConfigFromFile("/additional-service-configuration_serviceid-ssl-ciphers-only.yml", null);

        ApiMediationServiceConfig apiMediationServiceConfig = new ApiMediationServiceConfigReader().mergeConfigurations(apimlServcieConfig1, apimlServcieConfig2);
        assertNotNull(apiMediationServiceConfig);
        assertEquals("hellopje", apiMediationServiceConfig.getServiceId());
        assertEquals("keystore/localhost/localhost.keystore.p12", apiMediationServiceConfig.getSsl().getKeyStore());
        assertArrayEquals("password".toCharArray(), apiMediationServiceConfig.getSsl().getKeyStorePassword());
        assertEquals("keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());
        assertArrayEquals("password".toCharArray(), apiMediationServiceConfig.getSsl().getTrustStorePassword());
    }

    @Test
    void testGetApiMediationServiceConfigFromFile() throws ServiceDefinitionException {
        Map<String, String> properties = new HashMap<>();
        ApiMediationServiceConfig config = getApiMediationServiceConfigFromFile("/service-configuration.yml", properties);
        assertNotNull(config);

        config = getApiMediationServiceConfigFromFile(null, properties);
        assertNull(config);
    }

    private ApiMediationServiceConfig getApiMediationServiceConfigFromFile(String fileName, Map<String, String> properties) throws ServiceDefinitionException {

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        return apiMediationServiceConfigReader.buildConfiguration(fileName, properties);
    }

    private void checkContextMap(Map<String, String> contextMap) {
        assertNotNull(contextMap);
        assertNull(contextMap.get("NOT-AN-apiml.config.location"));
        assertEquals("/service-configuration.yml", contextMap.get("apiml.config.location"));
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
    void testSetApiMlContext_Ok() {
        ServletContext context = getMockServletContext();

        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        Map<String, String> contextMap = reader.setApiMlServiceContext(context);
        checkContextMap(contextMap);
    }

    @Test
    void testSetApiMlContextTwice_Ok() {
        ServletContext context = getMockServletContext();

        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        Map<String, String> contextMap = reader.setApiMlServiceContext(context);

        checkContextMap(contextMap);
    }

    @Test
    void testLoadConfigurationFromContext_OK() throws ServiceDefinitionException {
        ServletContext context = getMockServletContext();
        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig config = reader.loadConfiguration(context);
        assertNotNull(config);
    }

    @Test
    void testLoadConfigurationFromSingleFile_OK() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig config = reader.loadConfiguration("/service-configuration.yml");
        assertNotNull(config);
    }

    private ServletContext getMockServletContext() {
        ServletContext context = new MockServletContext();
        context.setInitParameter("NOT-AN-apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.location", "/service-configuration.yml");
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
    void testReadConfigurationFile_Internal_file_name_null() throws ServiceDefinitionException {

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        // 1) Existing file
        String internalFileName = "/service-configuration.yml";
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(null, internalFileName);
        assertNotNull(result);
    }

    @Test
    void testLoadConfiguration_IpAddressIsNull_OK() throws ServiceDefinitionException {
        String internalFileName = "/additional-service-configuration_ip-address-null.yml";
        String additionalFileName = "/additional-service-configuration_ip-address-null.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertNotNull(result);
        assertEquals("service", result.getServiceId());
    }

    @Test
    void testLoadConfiguration_IpAddressIsNull_bad_baseUrl() {
        String internalFileName = "/additional-service-configuration_ip-address-null_bad-baseUrl.yml";
        String additionalFileName = "/additional-service-configuration_ip-address-null_bad-baseUrl.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        Exception exception = assertThrows(ServiceDefinitionException.class,
            () -> apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName));

        assertThat(exception.getCause(), instanceOf(MalformedURLException.class));
    }

    @Test
    void testLoadConfiguration_IpAddressIsNull_UnknownHost() {
        String internalFileName = "/additional-service-configuration_ip-address-null_UnknownHost.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        Exception exception = assertThrows(ServiceDefinitionException.class,
            () -> apiMediationServiceConfigReader.loadConfiguration(internalFileName, null));

        assertThat(exception.getCause(), instanceOf(UnknownHostException.class));
    }
}
