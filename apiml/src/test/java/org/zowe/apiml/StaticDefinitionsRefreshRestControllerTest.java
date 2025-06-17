/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.zowe.apiml.discovery.staticdef.StaticRegistrationResult;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StaticDefinitionsRefreshRestControllerTest {

    @Mock
    private StaticServicesRegistrationService service;

    @InjectMocks
    private StaticDefinitionsRefreshRestController controller;

    @Nested
    class GivenRegistry {

        @Test
        void whenList_thenComplete() {
            when(service.getStaticInstances()).thenReturn(List.of());

            StepVerifier.create(controller.list())
                .expectNextMatches(entity -> entity.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(200)))
                .expectComplete()
                .verify();
        }

        @Test
        void whenRefresh_thenComplete() {
            var result = new StaticRegistrationResult();
            when(service.reloadServices()).thenReturn(result);

            StepVerifier.create(controller.reload())
                .expectNextMatches(entity ->
                    entity.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(200))
                    && entity.getBody().equals(result))
                .expectComplete()
                .verify();
        }

    }

}
