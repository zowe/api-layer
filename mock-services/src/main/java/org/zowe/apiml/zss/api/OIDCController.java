/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zss.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.zss.model.MapperResponse;
import org.zowe.apiml.zss.model.OIDCRequest;
import org.zowe.apiml.zss.model.ZssResponse;
import org.zowe.apiml.zss.services.OIDCProvider;

@RestController
@RequiredArgsConstructor
public class OIDCController {

    private final OIDCProvider provider;

    private boolean isCustomResponseRequired = false;
    private MapperResponse customResponse;
    private HttpStatus customHttpStatus;

    /**
     * Mock the distributed identity mapping response.
     * Error states are simulated with setting up custom response and status. These can be
     * initialized by request to /certificate/dn/mock-response
     *
     * @param oidcRequest OIDCRequest information used for the mapping.
     * @return Appropriate response code consistent with the one returned by the real SAF.
     * - 201 - Valid mapper response that will get validated afterwards by the OIDC Auth Source.
     */
    @PostMapping(value = "/certificate/dn")
    public ResponseEntity<MapperResponse> mockDistributedIdentityMapping(
        @RequestBody OIDCRequest oidcRequest) {

        if (isCustomResponseRequired) {
            isCustomResponseRequired = false;
            return new ResponseEntity<>(customResponse, customHttpStatus);
        }

        MapperResponse mapperResponse = provider.mapUserIdentity(oidcRequest);
        return new ResponseEntity<>(mapperResponse, HttpStatus.CREATED);
    }

    /**
     * Special endpoint which allows to set custom mapper response and status code.
     *
     * @param zssResponse ZssResponse defines what zss situation should be emulated
     * @return 201
     */
    @PostMapping(value = "/certificate/dn/mock-response")
    public ResponseEntity<String> setResponse(@RequestBody ZssResponse zssResponse) {
        isCustomResponseRequired = true;
        customHttpStatus = provider.setCustomStatus(zssResponse.getStatusCode());
        if (customHttpStatus.equals(HttpStatus.OK)) {
            customResponse = provider.setCustomResponse(zssResponse.getZssError());
        } else {
            customResponse = null;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Mocked response has been set");
    }
}
