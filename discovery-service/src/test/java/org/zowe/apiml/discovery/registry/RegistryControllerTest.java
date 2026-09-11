/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The /eureka HTTP surface, asserted against the contract captured from the Eureka-based service.
 * <p>
 * Standalone MockMvc rather than a Spring context: this exercises the controller and the registry together with
 * no servlet container and no Jersey, so it can run while the Eureka implementation is still wired into the
 * application. The status codes and representation rules below come from
 * {@code apiml-registry/src/test/resources/wire-contract/http-contract.json}.
 */
class RegistryControllerTest {

    private static final long NOW = 1_700_000_000_000L;

    private InMemoryServiceRegistry registry;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), () -> NOW);
        mvc = MockMvcBuilders.standaloneSetup(new RegistryController(registry, new org.zowe.apiml.registry.codec.RegistryCodec())).build();
        registry.register(discoverableClient(), RegistrationKind.DYNAMIC);
    }

    private ServiceInstance discoverableClient() {
        return ServiceInstance.builder()
            .instanceId("localhost:discoverableclient:10012")
            .appName("DISCOVERABLECLIENT")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10012, true)
            .vipAddress("discoverableclient")
            .secureVipAddress("discoverableclient")
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, NOW))
            .putMetadata("apiml.service.title", "Discoverable Client")
            .build();
    }

    // -----------------------------------------------------------------------------------------------------
    // Representation selection - the measured rules
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Representation {

        @Test
        @DisplayName("no Accept header yields XML, because that is what the Eureka-based service does")
        void defaultsToXml() throws Exception {
            mvc.perform(get("/eureka/apps"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/xml"))
                .andExpect(content().string(containsString("<applications>")));
        }

        @Test
        @DisplayName("Accept: */* also yields XML - a wildcard is not an explicit JSON request")
        void wildcardAcceptAlsoYieldsXml() throws Exception {
            mvc.perform(get("/eureka/apps").header("Accept", "*/*"))
                .andExpect(content().contentType("application/xml"));
        }

        @Test
        void onlyAnExplicitJsonRequestYieldsJson() throws Exception {
            mvc.perform(get("/eureka/apps").header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(containsString("\"applications\"")));
        }

        @Test
        void compactHeaderSelectsTheReducedProjection() throws Exception {
            String full = mvc.perform(get("/eureka/apps").header("Accept", "application/json"))
                .andReturn().getResponse().getContentAsString();
            String compact = mvc.perform(get("/eureka/apps")
                    .header("Accept", "application/json")
                    .header(RegistryRepresentation.EUREKA_ACCEPT_HEADER, "compact"))
                .andReturn().getResponse().getContentAsString();

            assertEquals(true, compact.length() < full.length(), "compact must be smaller than full");
            assertEquals(false, compact.contains("leaseInfo"), "compact drops the lease");
            assertEquals(true, full.contains("leaseInfo"));
        }

        @Test
        @DisplayName("an unknown ?regions= is accepted and ignored rather than rejected")
        void toleratesTheRegionsParameter() throws Exception {
            mvc.perform(get("/eureka/apps").param("regions", "eu-west-1"))
                .andExpect(status().isOk());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // The empty 404 - contractual
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class NotFoundResponses {

        @Test
        @DisplayName("a missed read returns 404 with an empty body and no Content-Type")
        void missedReadsReturnAnEmpty404() throws Exception {
            for (String path : List.of(
                "/eureka/apps/NOSUCHAPP",
                "/eureka/apps/unknown-service-id/unknown-instance-id",
                "/eureka/instances/unknown-instance-id"
            )) {
                var response = mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(emptyString()))
                    .andReturn().getResponse();
                assertNull(response.getContentType(), path + " must not carry a Content-Type");
            }
        }

        @Test
        @DisplayName("a missed write returns 404 with an empty body - enablers use this to detect they must re-register")
        void missedWritesReturnAnEmpty404() throws Exception {
            mvc.perform(put("/eureka/apps/unknown-service-id/unknown-instance-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(emptyString()));
            mvc.perform(delete("/eureka/apps/unknown-service-id/unknown-instance-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(emptyString()));
            mvc.perform(put("/eureka/apps/unknown-service-id/unknown-instance-id/status")
                    .param("value", "OUT_OF_SERVICE"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(emptyString()));
            mvc.perform(delete("/eureka/apps/unknown-service-id/unknown-instance-id/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(emptyString()));
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Lifecycle over HTTP
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Lifecycle {

        @Test
        void registersFromAJsonBody() throws Exception {
            String payload = """
                {"instance":{"instanceId":"localhost:newservice:10099","app":"NEWSERVICE",\
                "hostName":"localhost","ipAddr":"127.0.0.1","vipAddress":"newservice","status":"UP",\
                "port":{"$":10099,"@enabled":"true"}}}""";

            mvc.perform(post("/eureka/apps/NEWSERVICE")
                    .contentType("application/json")
                    .content(payload))
                .andExpect(status().isNoContent());

            assertEquals(true, registry.instance("NEWSERVICE", "localhost:newservice:10099").isPresent());
        }

        @Test
        @DisplayName("registers from an XML body, which is what the documented direct-onboarding path sends")
        void registersFromAnXmlBody() throws Exception {
            String payload = """
                <instance><instanceId>localhost:xmlservice:10098</instanceId><app>XMLSERVICE</app>\
                <hostName>localhost</hostName><ipAddr>127.0.0.1</ipAddr><vipAddress>xmlservice</vipAddress>\
                <status>UP</status><port enabled="true">10098</port></instance>""";

            mvc.perform(post("/eureka/apps/XMLSERVICE")
                    .contentType("application/xml")
                    .content(payload))
                .andExpect(status().isNoContent());

            var stored = registry.instance("XMLSERVICE", "localhost:xmlservice:10098").orElseThrow();
            assertEquals(10098, stored.port().port());
            assertEquals(true, stored.port().enabled());
        }

        @Test
        void renewsCancelsAndOverridesAKnownInstance() throws Exception {
            mvc.perform(put("/eureka/apps/DISCOVERABLECLIENT/localhost:discoverableclient:10012"))
                .andExpect(status().isOk());

            mvc.perform(put("/eureka/apps/DISCOVERABLECLIENT/localhost:discoverableclient:10012/status")
                    .param("value", "OUT_OF_SERVICE"))
                .andExpect(status().isOk());
            assertEquals(InstanceStatus.OUT_OF_SERVICE,
                registry.instance("DISCOVERABLECLIENT", "localhost:discoverableclient:10012")
                    .orElseThrow().status());

            mvc.perform(delete("/eureka/apps/DISCOVERABLECLIENT/localhost:discoverableclient:10012/status")
                    .param("value", "UP"))
                .andExpect(status().isOk());

            mvc.perform(delete("/eureka/apps/DISCOVERABLECLIENT/localhost:discoverableclient:10012"))
                .andExpect(status().isOk());
            assertEquals(0, registry.size());
        }

        @Test
        void mergesMetadataFromQueryParameters() throws Exception {
            mvc.perform(put("/eureka/apps/DISCOVERABLECLIENT/localhost:discoverableclient:10012/metadata")
                    .param("apiml.externalUrl", "https://example.org"))
                .andExpect(status().isOk());

            var stored = registry.instance("DISCOVERABLECLIENT", "localhost:discoverableclient:10012")
                .orElseThrow();
            assertEquals("https://example.org", stored.metadata().get("apiml.externalUrl"));
            assertEquals("Discoverable Client", stored.metadata().get("apiml.service.title"),
                "existing metadata must survive the merge");
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Lookups and replication
    // -----------------------------------------------------------------------------------------------------

    @Test
    void servesVipAndSecureVipLookups() throws Exception {
        mvc.perform(get("/eureka/vips/discoverableclient").header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("DISCOVERABLECLIENT")));
        mvc.perform(get("/eureka/svips/discoverableclient").header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("DISCOVERABLECLIENT")));
    }

    @Test
    @DisplayName("a replicated heartbeat for an unknown instance answers 404, asking the peer to re-register")
    void replicationReportsPerItemStatus() throws Exception {
        String batch = """
            {"replicationList":[\
            {"appName":"DISCOVERABLECLIENT","id":"localhost:discoverableclient:10012","action":"Heartbeat"},\
            {"appName":"GHOST","id":"localhost:ghost:1","action":"Heartbeat"}]}""";

        String response = mvc.perform(post("/eureka/peerreplication/batch/")
                .contentType("application/json")
                .content(batch))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andReturn().getResponse().getContentAsString();

        assertEquals(true, response.contains("\"statusCode\":200"));
        assertEquals(true, response.contains("\"statusCode\":404"),
            "the unknown instance must come back as 404 so the sender replicates a full registration");
    }

    @Test
    void appliesAReplicatedRegistration() throws Exception {
        String batch = """
            {"replicationList":[{"appName":"PEERSVC","id":"localhost:peersvc:10050","action":"Register",\
            "instanceInfo":{"instanceId":"localhost:peersvc:10050","app":"PEERSVC","hostName":"localhost",\
            "ipAddr":"127.0.0.1","vipAddress":"peersvc","status":"UP","port":{"$":10050,"@enabled":"true"}}}]}""";

        mvc.perform(post("/eureka/peerreplication/batch/")
                .contentType("application/json")
                .content(batch))
            .andExpect(status().isOk());

        assertEquals(true, registry.instance("PEERSVC", "localhost:peersvc:10050").isPresent());
    }

}
