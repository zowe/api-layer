/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The model carries no Jackson annotations, and its accessors are record-style rather than bean-style, so a
 * general-purpose mapper sees nothing. This pins both halves of that: invisible without the module, and in the
 * registry's own shape with it - the static-definition refresh body depends on the second.
 */
class RegistryJacksonModuleTest {

    private static ServiceInstance instance() {
        return ServiceInstance.builder()
            .instanceId("localhost:service:10010")
            .appName("SERVICE")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10010, true)
            .vipAddress("service")
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, System.currentTimeMillis()))
            .build();
    }

    @Test
    void givenNoModule_whenAnInstanceIsSerialised_thenTheAccessorsStayInvisible() {
        // Jackson does not quietly write an empty object here, it refuses outright: with no bean-style accessors
        // and no annotations there is nothing to discover. That is the failure the module exists to prevent.
        assertThatThrownBy(() -> new ObjectMapper().writeValueAsString(instance()))
            .isInstanceOf(InvalidDefinitionException.class);
    }

    @Test
    void givenTheModule_whenAnInstanceIsSerialised_thenTheRegistryFieldsAreWritten() throws Exception {
        var mapper = JsonMapper.builder().addModule(new RegistryJacksonModule()).build();

        var json = mapper.writeValueAsString(instance());

        assertThat(json)
            .contains("\"instanceId\"")
            .contains("localhost:service:10010")
            .contains("\"ipAddr\"")
            .contains("127.0.0.1");
    }

    @Test
    void givenTheModule_whenAnInstanceIsWrittenAndReadBack_thenItSurvives() throws Exception {
        var mapper = JsonMapper.builder().addModule(new RegistryJacksonModule()).build();

        var read = mapper.readValue(mapper.writeValueAsString(instance()), ServiceInstance.class);

        assertThat(read.instanceId()).isEqualTo("localhost:service:10010");
        assertThat(read.appName()).isEqualTo("SERVICE");
        assertThat(read.ipAddr()).isEqualTo("127.0.0.1");
        assertThat(read.port().port()).isEqualTo(10010);
        assertThat(read.status()).isEqualTo(InstanceStatus.UP);
    }
}
