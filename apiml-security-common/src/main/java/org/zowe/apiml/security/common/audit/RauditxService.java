/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.audit;

import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

public class RauditxService {

    public Optional<Rauditx> createInstance() {
        return ClassOrDefaultProxyUtils.createProxy(
            Rauditx.class,
            "com.ibm.jzos.Rauditx",
            () -> throw new IllegalStateException("Cannot create instance of Rauditx. The JZos library is not on the path")
        );
    }

}
