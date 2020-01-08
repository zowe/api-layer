/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.enable.register;

import com.ca.apiml.enable.EnableApiDiscovery;
import com.ca.apiml.enable.config.ApiMediationServiceConfigBean;
import com.ca.apiml.enable.config.SslConfigBean;
import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.product.registry.EurekaClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@EnableApiDiscovery
@ContextConfiguration(classes = EurekaClientWrapper.class)
public class ApimlDisabledRegisterToApiLayerTest {

    @Autowired
    private RegisterToApiLayer registerToApiLayer;

    @Autowired
    private ApiMediationServiceConfigBean apiMediationServiceConfigBean;

    @Autowired
    private SslConfigBean sslConfigBean;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Before
    public void setup() {
        registerToApiLayer = new RegisterToApiLayer(
            apiMediationServiceConfigBean, sslConfigBean);
    }


    @Test
    public void testOnContextRefreshedEventEvent() throws ServiceDefinitionException {
        registerToApiLayer.onContextRefreshedEventEvent();

        assertNotNull("ApiMediationServiceConfigBean is null", apiMediationServiceConfigBean);
        assertNotNull("SslConfigBean is null", sslConfigBean);

        verify(apiMediationClient, never()).register(apiMediationServiceConfigBean);
    }
}
