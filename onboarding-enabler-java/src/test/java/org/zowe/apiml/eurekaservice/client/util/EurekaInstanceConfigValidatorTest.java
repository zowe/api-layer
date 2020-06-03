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
}
