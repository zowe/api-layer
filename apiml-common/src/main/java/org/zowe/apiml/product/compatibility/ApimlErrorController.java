/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.compatibility;

import org.springframework.boot.web.servlet.error.ErrorController;

/**
 * This class is used to reconcile the breaking change between Spring Boot 2.5 and Zuul. The breaking change
 * is due to ErrorController.getErrorPath being removed in Spring Boot 2.5. A BeanPostProcessor is used
 * to proxy ZuulHandlerMapping, intercepting the code execution that leads to the NoSuchMethodError.
 */
public interface ApimlErrorController extends ErrorController {
    String getErrorPath();
}
