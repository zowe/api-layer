/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The endpoint the integration startup check reads on every instance.
 * <p>
 * Every service used to answer this from {@code apiml-common}; the Gateway, ZAAS and the API Catalog stopped
 * answering it when the endpoint moved to the Discovery Service, and the startup check then reported them as
 * never coming up - which gated every integration test job.
 */
class RegistryClientVersionEndpointTest {

    private static RegistryClientVersionEndpoint endpointFor(String appsHashCode) {
        RegistryClient client = new RegistryClient(new NoTransport());
        client.cache().replace(new Applications(List.of(), 1L, appsHashCode));
        return new RegistryClientVersionEndpoint(client);
    }

    @Test
    @DisplayName("The version is the count of UP instances, parsed from the hash code")
    void thenTheUpCountIsReported() {
        // Deliberately not the registry's own monotonic version: the startup check compares this value between
        // instances, and the Eureka-based clients still derive it from a hash code.
        assertEquals(3L, endpointFor("UP_3_").status().getVersion());
    }

    @Test
    @DisplayName("A hash code without an UP count reports -1, as before the first registry update")
    void thenNoUpCountReportsNotReady() {
        assertEquals(-1L, endpointFor("DOWN_0_").status().getVersion());
    }

    @Test
    @DisplayName("An empty view reports -1 rather than failing the request")
    void thenAnEmptyHashCodeIsNotAnError() {
        assertEquals(-1L, new RegistryClientVersionEndpoint(new RegistryClient(new NoTransport()))
            .status().getVersion());
    }

    private static class NoTransport implements RegistryTransport {

        @Override
        public Applications fetchApplications() {
            return new Applications(List.of(), 0L, "");
        }

        @Override
        public Applications fetchDelta() {
            return null;
        }

        @Override
        public void register(ServiceInstance instance) {
            // nothing to do
        }

        @Override
        public boolean renew(String appName, String instanceId) {
            return true;
        }

        @Override
        public void cancel(String appName, String instanceId) {
            // nothing to do
        }

        @Override
        public void updateStatus(String appName, String instanceId, InstanceStatus status) {
            // nothing to do
        }

    }

}
