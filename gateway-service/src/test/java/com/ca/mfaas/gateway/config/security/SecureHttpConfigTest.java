/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.security;

import com.ca.mfaas.gateway.security.config.WebSecurityConfig;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanInitializationException;

@RunWith(MockitoJUnitRunner.class)
public class SecureHttpConfigTest {
    private static final String INCORRECT_KEYRING = "safkeyring://user/ring";
    private static final String TRUSTSTORE = "localhost_truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "trustword";
    private static final String TRUSTSTORE_TYPE = "JKS";

    @Mock(
        answer = Answers.RETURNS_DEEP_STUBS
    )
    private MFaaSConfigPropertiesContainer propertiesContainer;

    private com.ca.mfaas.gateway.security.config.WebSecurityConfig securityConfig;


    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.when(this.propertiesContainer.getGateway().getVerifySslCertificatesOfServices()).thenReturn(true);

        this.securityConfig = new WebSecurityConfig(null, null,
            null, null, null,
            this.propertiesContainer, null);
    }

    @Test
    public void checkIncorrectSlashesInKeyRing() {
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStore()).thenReturn(INCORRECT_KEYRING);

        this.exceptionRule.expect(BeanInitializationException.class);
        this.exceptionRule.expectMessage("safkeyring://user/ring");

        this.securityConfig.secureHttpClient();
    }

    @Test
    public void checkTruststore() {
        String pathToTruststore = this.getClass().getClassLoader().getResource(TRUSTSTORE).getFile();

        Mockito.when(this.propertiesContainer.getSecurity().getTrustStore()).thenReturn(pathToTruststore);
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStorePassword()).thenReturn(TRUSTSTORE_PASSWORD);
        Mockito.when(this.propertiesContainer.getSecurity().getTrustStoreType()).thenReturn(TRUSTSTORE_TYPE);

        CloseableHttpClient httpClient = this.securityConfig.secureHttpClient();

        Assert.assertNotNull(httpClient);
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStore();
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStorePassword();
        Mockito.verify(this.propertiesContainer.getSecurity(), Mockito.times(1)).getTrustStoreType();
    }
}
