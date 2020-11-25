/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zfile;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class RcException extends RuntimeException {
    protected final String msg;
    protected final int rc;

    public RcException(String msg, int rc) {
        this.msg = msg;
        this.rc = rc;
    }
}
