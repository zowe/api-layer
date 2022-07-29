/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.Getter;

public class AccessControlError extends RuntimeException {

    private static final long serialVersionUID = 1678489927459963894L;

    @Getter
    private final PlatformReturned platformReturned;

    public AccessControlError(PlatformReturned platformReturned, String message) {
        super(message);
        this.platformReturned = platformReturned;
    }

}
