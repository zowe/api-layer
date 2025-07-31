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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import reactor.test.StepVerifier;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticRegistrationServiceApiTest {

    @Mock
    private StaticServicesRegistration staticServicesRegistration;

    @Test
    void givenService_whenRefresh_thenGenerateResponse() {
        var staticServiceApi = new StaticRegistrationServiceApi(staticServicesRegistration);
        doReturn(new StaticRegistrationResult()).when(staticServicesRegistration).reloadServices();

        StepVerifier.create(staticServiceApi.refresh())
            .assertNext(response -> {
                assertEquals(SC_OK, response.getStatusCode());
                assertEquals("{\"errors\":[],\"instances\":[],\"additionalServiceMetadata\":{},\"registeredServices\":[]}", response.getBody());
            })
            .verifyComplete();
    }

    @Test
    void givenInvalidObject_whenRefresh_thenThrowAnException() throws JsonProcessingException {
        var mapper = spy(new ObjectMapper());
        var staticServiceApi = new StaticRegistrationServiceApi(staticServicesRegistration);
        ReflectionTestUtils.setField(staticServiceApi, "mapper", mapper);
        doThrow(mock(JsonProcessingException.class)).when(mapper).writeValueAsString(any());

        StepVerifier.create(staticServiceApi.refresh())
            .expectError(IllegalStateException.class)
            .verify();
    }

}
