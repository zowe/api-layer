/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.zowe.apiml.eurekaservice.model.Health;

import java.util.HashMap;
import java.util.Map;

@Controller("/application")
public class HealthController {

    @Get(uri = "/health", produces = MediaType.APPLICATION_JSON)
    public Health getHealthStatus() {
        return new Health("UP");
    }

    @Get(uri = "/info", produces = MediaType.APPLICATION_JSON)
    public Map<String, String> getInfo() {
        return new HashMap<>();
    }
}
