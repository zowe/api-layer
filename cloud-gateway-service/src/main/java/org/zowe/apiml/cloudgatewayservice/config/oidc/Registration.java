/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/

package org.zowe.apiml.cloudgatewayservice.config.oidc;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class Registration {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private List<String> scope = Arrays.asList("openid", "profile", "email");

}
