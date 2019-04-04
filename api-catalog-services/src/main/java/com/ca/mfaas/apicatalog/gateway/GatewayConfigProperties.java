/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * Carries information of the Gateway scheme and port.
 */
@Builder
@Getter
public class GatewayConfigProperties {

    private String scheme;
    private String hostname;

}
