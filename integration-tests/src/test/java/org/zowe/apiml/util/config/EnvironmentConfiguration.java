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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentConfiguration {
    private Credentials credentials;
    private GatewayServiceConfiguration gatewayServiceConfiguration;
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private DiscoverableClientConfiguration discoverableClientConfiguration;
    private TlsConfiguration tlsConfiguration;
    private ZosmfServiceConfiguration zosmfServiceConfiguration;
    private AuxiliaryUserList auxiliaryUserList;
}
