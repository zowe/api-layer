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
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.*;

class EurekaInstanceConfigValidatorTest {

    private EurekaInstanceConfigValidator validator = new EurekaInstanceConfigValidator();
    private ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();

    @Test
    public void givenServiceConfiguration_whenConfigurationIsValid_thenValidate() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration.yml");
        assertDoesNotThrow(() -> validator.validate(testConfig));
    }

    @Test
    public void givenConfigurationWithInvalidProtocol_whenValidate_thenThrowException() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("bad-protocol-baseurl-service-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("'ftp' is not valid protocol for baseUrl property", exception.getMessage());
    }

    @Test
    public void givenConfigurationWithInvalidSsl_whenValidate_thenThrowException() throws ServiceDefinitionException {
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("bad-ssl-configuration.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("SSL configuration was not provided. Try add apiml.service.ssl section.", exception.getMessage());
    }

    @Test
    public void givenSystemProperties_whenLoadFromFile_thenNoOverrideBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");

        String internalFileName = "/service-configuration.yml";

        ApiMediationServiceConfig testConfig = configReader.loadConfiguration(internalFileName);
        validator.validate(testConfig);

        assertEquals("service", testConfig.getServiceId()); // no replace without wildcard
    }

    @Test
    public void givenSystemProperties_whenLoadFromContext_thenNotOverrideBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");
        ServletContext context = new MockServletContext();

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig testConfig = apiMediationServiceConfigReader.loadConfiguration(context);
        validator.validate(testConfig);

        assertEquals("service", testConfig.getServiceId()); // no replace without wildcard
    }

    @Test
    public void givenSystemProperties_whenLoadFromFileThatHasWildcardButPropsNotSetForMandatory_thenThrowException() throws Exception {
        // ssl.keystore has wildcard but is not set, exception will be thrown
        System.setProperty("apiml.serviceId", "veronica");
        System.clearProperty("apiml.keystore");
        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration-wildcard.yml");
        Exception exception = assertThrows(MetadataValidationException.class,
            () -> validator.validate(testConfig),
            "Expected exception is not MetadataValidationException");
        assertEquals("SSL parameters are missing or were not replaced by the system properties.", exception.getMessage());
    }

    @Test
    public void givenSystemProperties_whenLoadFromFileThatHasWildcard_thenConfigOverridenBySystemProp() throws Exception {
        System.setProperty("apiml.serviceId", "veronica");
        System.setProperty("prefix.description", "samantha");
        System.setProperty("apiml.keystore", "keystore");

        ApiMediationServiceConfig testConfig = configReader.loadConfiguration("service-configuration-wildcard.yml");
        validator.validate(testConfig);

        assertEquals("veronica", testConfig.getServiceId());    // wildcard is mandatory to replace
        assertEquals("${prefix.description}", testConfig.getDescription());  // it allows you to specify arbitraty prefix, yet only apiml prefix is replaced
        assertEquals("${apiml.title}", testConfig.getTitle());  // it leaves the unreplaced prefixes
    }
}
