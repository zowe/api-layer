/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import lombok.*;

/**
 * Configuration parameters for DiscoverableClient
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DiscoverableClientConfiguration extends ServiceConfiguration {

    private String applId;

    DiscoverableClientConfiguration(String scheme, String applId, String host, String port, int instances) {
        super(scheme, null, host, port, instances);
        this.applId = applId;
    }

    @Override
    public String getServiceId() {
        return "discoverableclient";
    }

    @Override
    public String getServletContext() {
        return "/" + getServiceId() + "/";
    }

    @Override
    public boolean isBasicAuthenticationSupported() {
        return false;
    }

}
