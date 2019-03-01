/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.security.config;

import com.broadcom.apiml.test.integration.error.ErrorService;
import com.broadcom.apiml.test.integration.rest.response.ApiMessage;
import com.broadcom.apiml.test.integration.rest.response.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class BeansConfigurationTest {
    private BeansConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new BeansConfiguration();
    }

    @Test
    public void errorServiceCreationTest() {
        String messageKey = "com.ca.mfaas.security.authenticationRequired";
        String url = "/api/v1";
        String messageText = String.format("Authentication is required for URL '%s'", url);

        ErrorService errorService = configuration.errorService();
        ApiMessage apiMessage = errorService.createApiMessage(messageKey, url);
        Message errorMessage = apiMessage.getMessages().get(0);

        assertThat(errorMessage.getMessageNumber(), is("SEC0001"));
        assertThat(errorMessage.getMessageContent(), is(messageText));
    }

    @Test
    public void objectMapperCreationTest() {
        ObjectMapper mapper = configuration.objectMapper();

        assertNotNull(mapper);
    }
}
