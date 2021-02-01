/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.apars;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zowe.apiml.client.services.versions.Apar;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PH30398 implements Apar {
    @Value("${zosmf.username}")
    private List<String> usernames;
    @Value("${zosmf.password}")
    private List<String> passwords;

    @Override
    public Optional<ResponseEntity<?>> apply(Object... parameters) {
        String calledService = (String) parameters[0];

        if (calledService.equals("authentication")) {
            Map<String, String> headers = (Map<String, String>) parameters[4];

            String authorization = headers.get("authorization");
            if (authorization == null || authorization.isEmpty()) {
                return Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            }
        }
        // TODO implement functionality other than checking for no authentication
        return (Optional<ResponseEntity<?>>) parameters[2];
    }
}
