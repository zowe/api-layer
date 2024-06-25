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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zowe.apiml.client.model.LoginBody;
import org.zowe.apiml.client.services.apars.Apar;
import org.zowe.apiml.client.services.versions.Versions;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@SuppressWarnings("squid:S1452")
public class AparBasedService {
    private final String baseVersion;
    private final List<String> appliedApars;
    private final Versions versions;

    public AparBasedService(@Value("${zosmf.baseVersion}") String baseVersion, @Value("${zosmf.appliedApars}") List<String> appliedApars, Versions versions) {
        this.baseVersion = baseVersion;
        this.appliedApars = appliedApars;
        this.versions = versions;
        log.info("baseVersion: {}", baseVersion);
        log.info("appliedApars: {}", appliedApars);
        log.info("versions: {}", versions);
        log.info("fullSetOfApplied: {}", versions.fullSetOfApplied(baseVersion, appliedApars));
    }

    public ResponseEntity<?> process(String calledService, String calledMethods, HttpServletResponse response, Map<String, String> headers, Object ... parameters) {
        try {
            List<Apar> applied = versions.fullSetOfApplied(baseVersion, appliedApars);
            log.info("calledService: {}, calledMethods, {}", calledService, calledMethods);
            Optional<ResponseEntity<?>> result = Optional.empty();
            for (Apar apar : applied) {
                log.info("applying: {}", apar);
                if (parameters.length > 0) {
                    LoginBody body = (LoginBody) parameters[0];
                    result = apar.apply(calledService, calledMethods, result, response, headers, body);
                }
                else {
                    result = apar.apply(calledService, calledMethods, result, response, headers);
                }
                log.info("result: {}", result);
            }

            if (result.isPresent()) {
                ResponseEntity<?> finalResult = result.get();
                logResult(finalResult);
                return finalResult;
            } else {
                ResponseEntity<Object> finalResult = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                logResult(finalResult);
                return finalResult;
            }
        } catch (Exception e) {
            ResponseEntity<Object> finalResult = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            logResult(finalResult);
            return finalResult;
        }
    }

    private void logResult(Object o) {
        log.info("final result: {}", o);
    }
}
