/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

@RestController
@RequestMapping("/api/v1/zaasClient")
@Api(
    value = "/api/v1/zaasClient",
    consumes = "application/json",
    tags = {"Zaas client test call"})
public class ZaasClientTestController {

    private ZaasClient zaasClient;

    public ZaasClientTestController(ZaasClient zaasClient) {
        this.zaasClient = zaasClient;
    }

    @PostMapping(value = "/login")
    @ApiOperation(value = "Forward login to gateway service via zaas client")
    public ResponseEntity<String> forwardLogin(@RequestBody LoginRequest loginRequest) {
        try {
            String jwt = zaasClient.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok().body(jwt);
        } catch (ZaasClientException e) {
            return ResponseEntity.status(e.getErrorCode().getReturnCode()).body(e.getErrorCode().getMessage());
        }

    }

    @PostMapping(value = "/logout")
    @ApiOperation(value = "Forward logout to gateway service via zaas client")
    public ResponseEntity<String> forwardLogout(
        @CookieValue(value = "apimlAuthenticationToken", required = false) String cookieToken,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) throws ZaasConfigurationException {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(authorizationHeader)) {
            return ResponseEntity.status(500).body("Missing cookie or authorization header in the request");
        }

        try {
            String token = StringUtils.isEmpty(cookieToken) ? authorizationHeader : cookieToken;
            zaasClient.logout(token);
        } catch (ZaasClientException e) {
            return ResponseEntity.status(e.getErrorCode().getReturnCode()).body(e.getErrorCode().getMessage());
        }
        return ResponseEntity.noContent().build();
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class LoginRequest {
    private String username;
    private String password;

}

