/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static junit.framework.TestCase.assertFalse;

public class ApiMediationClientServiceTest {
    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void registerTest() throws ServiceDefinitionException {
        ApiMediationClientService apiMediationClientService = new ApiMediationClientService();
        apiMediationClientService.register();
    }

    @Test
    public void registerTest_duplicate() throws ServiceDefinitionException {
        exceptionRule.expect(ServiceDefinitionException.class);
        ApiMediationClientService apiMediationClientService = new ApiMediationClientService();
        apiMediationClientService.register();
        apiMediationClientService.register();
    }

    @Test
    public void isRegisteredTest_notRegistered() {
        ApiMediationClientService apiMediationClientService = new ApiMediationClientService();
        assertFalse(apiMediationClientService.isRegistered());
    }

    @Test
    public void unregisterTest() {
        ApiMediationClientService apiMediationClientService = new ApiMediationClientService();
        apiMediationClientService.unregister();
    }

    @Test
    public void unregisterTest_notRegistered() {
        ApiMediationClientService apiMediationClientService = new ApiMediationClientService();
        apiMediationClientService.unregister();
    }
}
