/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.saf;

import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import static org.zowe.apiml.util.ClassOrDefaultProxyUtils.ClassOrDefaultProxyState;
import static org.zowe.apiml.util.ClassOrDefaultProxyUtils.createProxy;

public class SafPlatformClassFactory implements PlatformClassFactory {
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    public Class<?> getPlatformUserClass() throws ClassNotFoundException {
        PlatformUser proxy = createProxy(PlatformUser.class, "com.ibm.os390.security.PlatformUser", MockPlatformUser::new);
        if (!((ClassOrDefaultProxyState) proxy).isUsingBaseImplementation()) {
            apimlLog.log("org.zowe.apiml.security.loginEndpointInDummyMode", MockPlatformUser.VALID_USERID, MockPlatformUser.VALID_PASSWORD);
        }
        return proxy.getClass();
    }

    @Override
    public Class<?> getPlatformReturnedClass() throws ClassNotFoundException {
        Class<?> aClass;
        try {
            aClass = Class.forName("com.ibm.os390.security.PlatformReturned");
        } catch (ClassNotFoundException e) {
            aClass = Class.forName("org.zowe.apiml.gateway.security.login.saf.PlatformReturned");
        }
        return aClass;
    }

    @Override
    public Object getPlatformUser() {
        return null;
    }
}
