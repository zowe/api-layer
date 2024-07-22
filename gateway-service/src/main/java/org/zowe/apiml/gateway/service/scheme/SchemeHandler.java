/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service.scheme;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

/**
 * This interface is used by handler which add filter to transform credentials in the request.
 */
public interface SchemeHandler {

    /**
     * @return type of supported authentication scheme
     */
    AuthenticationScheme getAuthenticationScheme();

    /**
     * Set filter in the routing rule.
     * @param routeDefinition rule to be updated
     * @param auth definition of authentication scheme from the service instance
     */
    void apply(ServiceInstance serviceInstance, RouteDefinition routeDefinition, Authentication auth);

}
