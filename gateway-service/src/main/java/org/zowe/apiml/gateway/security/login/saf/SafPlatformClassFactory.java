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

public class SafPlatformClassFactory implements PlatformClassFactory {

    @Override
    public Class<?> getPlatformUserClass() throws ClassNotFoundException {
        return Class.forName("com.ibm.os390.security.PlatformUser");
    }

    @Override
    public Class<?> getPlatformReturnedClass() throws ClassNotFoundException {
        return Class.forName("com.ibm.os390.security.PlatformReturned");
    }

    @Override
    public Object getPlatformUser() {
        return null;
    }
}
