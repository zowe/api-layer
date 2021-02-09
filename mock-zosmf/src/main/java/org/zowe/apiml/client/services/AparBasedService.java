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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zowe.apiml.client.services.apars.Apar;
import org.zowe.apiml.client.services.versions.Versions;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("squid:S1452")
public class AparBasedService {
    private final String baseVersion;
    private final List<String> appliedApars;
    private final Versions versions;

    @Autowired
    public AparBasedService(@Value("${zosmf.baseVersion}") String baseVersion, @Value("${zosmf.appliedApars}") List<String> appliedApars, Versions versions) {
        this.baseVersion = baseVersion;
        this.appliedApars = appliedApars;
        this.versions = versions;
    }

    public ResponseEntity<?> process(String calledService, String calledMethods, HttpServletResponse response, Map<String, String> headers) {
        try {
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
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
