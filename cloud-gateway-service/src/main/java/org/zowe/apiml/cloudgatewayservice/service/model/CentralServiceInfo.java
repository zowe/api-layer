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

import com.netflix.appinfo.InstanceInfo;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class CentralServiceInfo {

    private final InstanceInfo.InstanceStatus status;
    private final Map<String,String> customMetadata;
    private final Set<String> apiId;
    private final String serviceId;

}
