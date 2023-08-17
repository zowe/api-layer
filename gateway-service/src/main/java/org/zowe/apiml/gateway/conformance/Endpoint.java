/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;


import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Set;

/**
 * This class contains all information necessary to call an endpoint through the Gateway
 */
@Data
public class Endpoint {
    /**
     * Url of the endpoint in the gateway
     */
    private final String url;
    /**
     * ServiceId of the service that owns the endpoint
     */
    private final String serviceId;
    /**
     * Method which the endpoint accepts
     */
    private final HttpMethod httpMethod;
    /**
     * Documented responses that the API can return, taken from the swagger, they are either of the form "400", "401"... or "default"
     */
    private final Set<String> validResponses;
}
