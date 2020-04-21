/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;
import org.zowe.apiml.security.common.auth.Authentication;

/**
 * Interface with base method to get AuthenticationCommand by serviceId or Authentication.
 *
 * {@see org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl}
 */
public interface ServiceAuthenticationService extends ServiceCacheEvict {

    /**
     * Get or create command to service's authentication using known Authentication object and jwtToken of current user
     * @param authentication Object describing authentication to the service
     * @param jwtToken JWT security token of user (authentication can depends on user privilege)
     * @return authentication command to update request in ZUUL
     */
    public AuthenticationCommand getAuthenticationCommand(Authentication authentication, String jwtToken);

    /**
     * Get or create command to service's authentication using serviceId and jwtToken of current user
     * @param serviceId ID of service to call
     * @param jwtToken JWT security token of user (authentication can depends on user privilege)
     * @return authentication command to update request in ZUUL (or lazy command to be updated in load balancer)
     */
    public AuthenticationCommand getAuthenticationCommand(String serviceId, String jwtToken);


    public Authentication getAuthentication(InstanceInfo instanceInfo);

}
