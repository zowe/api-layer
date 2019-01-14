/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.catalina.LifecycleException;
import org.junit.Test;

public class HttpsFactoryTest {

    @Test
    public void correctConfigurationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        String secret = httpsFactory.readSecret();
        assertNotNull(secret);
    }

    @Test(expected = HttpsConfigError.class)    
    public void wrongKeyPasswordConfigurationShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().keyPassword("WRONG").build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        String secret = httpsFactory.readSecret();
        assertNotNull(secret);
    }

    @Test
    public void specificCorrectAliasShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().keyAlias("localhost").build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        String secret = httpsFactory.readSecret();
        assertNotNull(secret);
    }

    @Test(expected = HttpsConfigError.class)    
    public void specificIncorrectAliasShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().keyAlias("INVALID").build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        String secret = httpsFactory.readSecret();
        assertNull(secret);
    }
}
