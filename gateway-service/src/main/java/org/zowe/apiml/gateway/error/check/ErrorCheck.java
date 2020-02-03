/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.error.check;

import org.zowe.apiml.message.api.ApiMessageView;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface ErrorCheck {
    ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc);
}
