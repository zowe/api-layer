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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.product.logging.LogMessageTracker;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.*;

class EurekaInstanceConfigValidatorTest {

    private final EurekaInstanceConfigValidator validator = new EurekaInstanceConfigValidator();
    private final ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();

    private final LogMessageTracker logTracker = new LogMessageTracker(validator.getClass());

    @BeforeEach
    void setup(){

    }

    @Test
    void givenServiceConfiguration_whenConfigurationIsValid_thenValidate() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration.yml");
        assertDoesNotThrow(() -> validator.validate(testConfig));
    }

    @Test
    void givenConfigurationWithInvalidSsl_whenValidate_thenThrowException() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("bad-ssl-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("SSL configuration was not provided. Try add apiml.service.ssl section.", exception.getMessage());
    }

    @Test
    void givenSystemProperties_whenLoadFromFile_thenNoOverrideBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");

        String internalFileName = "/service-configuration.yml";

        ApiMediationServiceConfig testConfig = configReader.loadConfiguration(internalFileName);
        validator.validate(testConfig);

        assertEquals("service", testConfig.getServiceId()); // no replace without wildcard
    }

    @Test
    void givenSystemProperties_whenLoadFromContext_thenNotOverrideBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");
        ServletContext context = new MockServletContext();

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig testConfig = apiMediationServiceConfigReader.loadConfiguration(context);
        validator.validate(testConfig);

        assertEquals("service", testConfig.getServiceId()); // no replace without wildcard
    }

    @Test
    void givenSystemProperties_whenLoadFromFileThatHasWildcardButPropsNotSetForMandatory_thenThrowException() throws Exception {
        // ssl.keystore has wildcard but is not set, exception will be thrown
        System.setProperty("apiml.serviceId", "veronica");
        System.clearProperty("apiml.keystore");
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration-wildcard.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("SSL parameters ** keyStore ** are missing or were not replaced by the system properties.", exception.getMessage());
    }

    @Test
    void givenSystemProperties_whenLoadFromFileThatHasWildcard_thenConfigOverridenBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");
        System.setProperty("prefix.description", "samantha");
        System.setProperty("apiml.keystore", "keystore");

        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration-wildcard.yml");
        validator.validate(testConfig);

        assertEquals("veronica", testConfig.getServiceId());    // wildcard is mandatory to replace
        assertEquals("${prefix.description}", testConfig.getDescription());  // it allows you to specify arbitraty prefix, yet only apiml prefix is replaced
        assertEquals("${apiml.title}", testConfig.getTitle());  // it leaves the unreplaced prefixes
    }

    @Test
    void givenConfigurationWrongRoutes_whenValidate_thenThrowException() throws Exception {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("wrong-routes-service-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("Routes parameters  ** gatewayUrl, serviceUrl ** are missing or were not replaced by the system properties.", exception.getMessage());
    }

    @Test
    void givenConfigurationEmptyCatalog_whenValidate_thenLog() throws Exception {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("empty-catalog-service-configuration.yml");
        validator.validate(testConfig);

        assertNull(testConfig.getCatalog());
    }

    @Test
    void givenConfigurationEmptyApiInfo_whenValidate_thenLog() throws Exception {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("empty-apiinfo-service-configuration.yml");
        validator.validate(testConfig);

        assertNull(testConfig.getApiInfo());
    }

    @Test
    void givenConfigurationWithMissingSslParams_whenValidate_thenThrowException() throws Exception {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("missing-ssl-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("SSL parameters ** protocol, trustStore, keyStore, keyAlias, keyStoreType, trustStoreType, trustStorePassword, keyStorePassword, keyPassword, enabled ** are missing or were not replaced by the system properties.", exception.getMessage());
    }

    @Test
    void givenConfigurationWithKeyring_whenOtherConfigurationIsValid_thenValidate() throws Exception {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("keyring-ssl-configuration.yml");
        assertDoesNotThrow(() -> validator.validate(testConfig));
    }
}
