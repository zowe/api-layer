/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import reactor.core.publisher.Mono;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(name = "modulithConfig")
public class StaticRegistrationServiceApi implements StaticRegistrationService {

    private ObjectMapper mapper = new ObjectMapper();
    private final StaticServicesRegistration staticServicesRegistration;

    @Override
    public Mono<StaticAPIResponse> refresh() {
        try {
            var result = staticServicesRegistration.reloadServices();
            return Mono.just(new StaticAPIResponse(SC_OK, mapper.writeValueAsString(result)));
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize the list of static API services", e);
            return Mono.error(new IllegalStateException(e));
        }
    }

}
