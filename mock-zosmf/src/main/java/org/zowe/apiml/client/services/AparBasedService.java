/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zowe.apiml.client.services.versions.Apar;
import org.zowe.apiml.client.services.versions.Versions;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AparBasedService {
    @Value("${zosmf.baseVersion}")
    String baseVersion;
    @Value("${zosmf.appliedApars}")
    String[] appliedApars;

    @Autowired
    private final Versions versions;

    public ResponseEntity<?> process(String calledService, String calledMethods, HttpServletResponse response, Map<String, String> headers) {
        List<Apar> applied = versions.fullSetOfApplied(baseVersion, appliedApars);

        Optional<ResponseEntity<?>> result = Optional.empty();
        for (Apar apar : applied) {
            result = apar.apply(calledService, calledMethods, result, response, headers);
        }

        if (result.isPresent()) {
            return result.get();
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
