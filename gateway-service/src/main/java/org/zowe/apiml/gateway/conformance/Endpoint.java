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


@Data
public class Endpoint {
    private final String url;
    private final String serviceId;
    private final HttpMethod httpMethod;
    private final Set<String> validResponses;
}
