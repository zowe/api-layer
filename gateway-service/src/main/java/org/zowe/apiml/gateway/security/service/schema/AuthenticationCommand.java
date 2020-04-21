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
import org.apache.http.HttpRequest;
import org.zowe.apiml.cache.EntryExpiration;

import java.io.Serializable;

/**
 * This command represented a code, which distribute right access right to a service. Gateway translates requests
 * to a service and by login in there generate or translate authentication to the service.
 *
 * Responsible for this translation is filter {@link org.zowe.apiml.gateway.filters.pre.ServiceAuthenticationFilter}
 */
public abstract class AuthenticationCommand implements EntryExpiration, Serializable {

    private static final long serialVersionUID = -4519869709905127608L;

    // TODO Authentication Command key should be here

    public static final AuthenticationCommand EMPTY = new AuthenticationCommand() {

        private static final long serialVersionUID = 5280496524123534478L;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            // do nothing
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isRequiredValidJwt() {
            return false;
        }

    };

    /**
     * Apply the command, if it is necessary, it is possible to use a specific instance for execution. This is
     * using for loadBalancer command, where are not available all information in step of command creation.
     * In all other case call apply(null).
     * @param instanceInfo Specific instanceIf if it is needed
     */
    public abstract void apply(InstanceInfo instanceInfo);

    /**
     * This method identify if for this authentication command, schema is required to be logged. Main purpose is
     * to make differences between bypass and other schema's type. Schema shouldn't change anything, but for some other
     * it is required be logged and send valid JWT token.
     * @return true is valid token is required, otherwise false
     */

    public abstract boolean isRequiredValidJwt();

    public void applyToRequest(HttpRequest request) {
        throw new UnsupportedOperationException();
    }
}
