/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.zosmf;

/**
 * General strategy for token validation
 *
 * toString method should be implemented as debug logs
 * call this method. It should provide information about
 * what principle will the strategy employ to validate
 * the request
 *
 */
public interface TokenValidationStrategy {
    void validate(TokenValidationRequest request);
}
