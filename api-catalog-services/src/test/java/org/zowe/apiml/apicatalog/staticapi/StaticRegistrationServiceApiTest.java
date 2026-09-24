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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;
import reactor.test.StepVerifier;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticRegistrationServiceApiTest {

    @Mock
    private StaticServicesRegistration staticServicesRegistration;

    @Test
    void givenService_whenRefresh_thenGenerateResponse() {
        var staticServiceApi = new StaticRegistrationServiceApi(new ObjectMapper(), staticServicesRegistration);
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
        var staticServiceApi = new StaticRegistrationServiceApi(mapper, staticServicesRegistration);
        ReflectionTestUtils.setField(staticServiceApi, "mapper", mapper);
        doThrow(mock(JsonProcessingException.class)).when(mapper).writeValueAsString(any());

        StepVerifier.create(staticServiceApi.refresh())
            .expectError(IllegalStateException.class)
            .verify();
    }

    /**
     * The refresh endpoint is driven by the registry's own model, and the registry model is deliberately free of
     * Jackson annotations - so this asserts the shape that the response body actually has to have, through the
     * mapper this service serialises with.
     * <p>
     * The failure this pins, seen in {@code CITestsModulith} as a 500 from
     * {@code POST /static-api/refresh}, {@code POST /apicatalog/api/v1/static-api/refresh} and
     * {@code GET /discovery/api/v1/staticApi}:
     * <pre>
     *   InvalidDefinitionException: No serializer found for class org.zowe.apiml.registry.model.ServiceInstance
     *   and no properties discovered to create BeanSerializer
     *   (through reference chain: StaticRegistrationResult["instances"]-&gt;LinkedList[0])
     * </pre>
     * A mapper without the registry module cannot see record-style accessors, so it reports the model as having
     * no properties at all. Note that the assertion is on the nested instance too: serialising the envelope
     * alone would pass even with the bug, because the chain breaks on the first element of {@code instances}.
     */
    @Test
    void givenStaticRegistrationResultWithInstance_whenSerialised_thenInstanceIsWrittenWithItsFields() throws JsonProcessingException {
        var staticServiceApi = new StaticRegistrationServiceApi(new ObjectMapper(), staticServicesRegistration);
        StaticRegistrationResult result = new StaticRegistrationResult();
        result.getInstances().add(instance());

        String body = serialiseWith(staticServiceApi, result);

        // Read it back with a mapper that knows nothing about the registry: the point is that the body is
        // ordinary JSON with the instance's fields in it, not that this module can read its own output.
        JsonNode tree = new ObjectMapper().readTree(body);

        // The envelope keeps its shape...
        assertEquals(0, tree.get("errors").size(), body);
        assertEquals(0, tree.get("registeredServices").size(), body);
        assertEquals(1, tree.get("instances").size(), body);

        // ...and the nested instance carries the fields the API Catalog and the startup check read. Without the
        // registry module on the mapper this element has no fields at all and the whole response is a 500.
        JsonNode written = tree.get("instances").get(0);
        assertEquals("static-mock-services:mockzosmf:10013", written.get("instanceId").asText(), body);
        assertEquals("MOCKZOSMF", written.get("app").asText(), body);
        assertEquals("mock-services", written.get("hostName").asText(), body);
        assertEquals("UP", written.get("status").asText(), body);
        assertEquals("https://mock-services:10013/", written.get("homePageUrl").asText(), body);
        // The port is an object with the value and the flag, and the flag is a string - not the bare number a
        // plain bean serializer would produce.
        assertEquals(10013, written.get("port").get("$").asInt(), body);
        assertEquals("true", written.get("port").get("@enabled").asText(), body);
        assertEquals(10013, written.get("securePort").get("$").asInt(), body);
        assertEquals("https://gateway-service:10010",
            written.get("metadata").get("apiml.service.gatewayUrl").asText(), body);
    }

    /**
     * Why the registry module has to be registered explicitly, rather than being a stylistic preference.
     * <p>
     * A mapper without it does not produce {@code {}} for the model, it refuses the model outright - the
     * {@code InvalidDefinitionException} that failed the modulith's three static-refresh endpoints. Asserting
     * this keeps the requirement visible: drop the module from
     * {@link StaticRegistrationServiceApi}'s constructor and the test above stops compiling the failure away.
     */
    @Test
    void givenRegistryModel_whenSerialisedWithAPlainMapper_thenItIsRefused() {
        ObjectMapper plain = new ObjectMapper();

        assertThrows(InvalidDefinitionException.class, () -> plain.writeValueAsString(instance()));
    }

    private static String serialiseWith(StaticRegistrationServiceApi service, StaticRegistrationResult result)
        throws JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) ReflectionTestUtils.getField(service, "mapper");
        return mapper.writeValueAsString(result);
    }

    private static ServiceInstance instance() {
        return ServiceInstance.builder()
            .instanceId("static-mock-services:mockzosmf:10013")
            .appName("MOCKZOSMF")
            .hostName("mock-services")
            .ipAddr("127.0.0.1")
            .port(new PortInfo(10013, true))
            .securePort(new PortInfo(10013, true))
            .vipAddress("mockzosmf")
            .secureVipAddress("mockzosmf")
            .status(InstanceStatus.UP)
            .homePageUrl("https://mock-services:10013/")
            .statusPageUrl("https://mock-services:10013/application/info")
            .secureHealthCheckUrl("https://mock-services:10013/application/health")
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(Lease.permanent(System.currentTimeMillis()))
            .metadata(java.util.Map.of("apiml.service.gatewayUrl", "https://gateway-service:10010"))
            .build();
    }

}
