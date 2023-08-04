/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.instance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EurekaServiceInstanceRequest {

    private String serviceId;
    private String eurekaRequestUrl;
    private String username;
    private String password;

}
