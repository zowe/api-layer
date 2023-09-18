/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApimlInfo {

    private final String apimlId;
    private final List<CentralServiceInfo> services;
}
