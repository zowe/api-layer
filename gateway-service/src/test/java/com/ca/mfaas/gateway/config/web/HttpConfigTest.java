/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.web;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanInitializationException;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class HttpConfigTest {
    private static final String INCORRECT_KEYRING = "safkeyring://user/ring";
    private static final String TRUSTSTORE = "localhost_truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "trustword";
    private static final String TRUSTSTORE_TYPE = "JKS";
    private MFaaSConfigPropertiesContainer propertiesContainer;

    private HttpConfig httpConfig;


    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        propertiesContainer = mock(MFaaSConfigPropertiesContainer.class, RETURNS_DEEP_STUBS);
        Mockito.when(this.propertiesContainer.getGateway().getVerifySslCertificatesOfServices()).thenReturn(true);

        this.httpConfig = new HttpConfig(propertiesContainer);
    }

    @Test
    public void checkIncorrectSlashesInKeyRing() {
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStore()).thenReturn(INCORRECT_KEYRING);

        this.exceptionRule.expect(BeanInitializationException.class);
        this.exceptionRule.expectMessage("safkeyring://user/ring");

        this.httpConfig.secureHttpClient();
    }

    @Test
    public void checkTruststore() {
        String pathToTruststore = this.getClass().getClassLoader().getResource(TRUSTSTORE).getFile();

        Mockito.when(this.propertiesContainer.getSecurity().getTrustStore()).thenReturn(pathToTruststore);
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStorePassword()).thenReturn(TRUSTSTORE_PASSWORD);
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStoreType()).thenReturn(TRUSTSTORE_TYPE);

        CloseableHttpClient httpClient = this.httpConfig.secureHttpClient();

        Assert.assertNotNull(httpClient);
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStore();
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStorePassword();
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStoreType();
    }
}
