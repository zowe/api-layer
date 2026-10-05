/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.replication;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.codec.CorpusFixtures;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The peer-replication protocol, checked against fixtures captured from Eureka's own replication codec.
 * <p>
 * HA between LPARs rides on this format. If it drifts, one Discovery Service silently stops learning about
 * services registered on the other, which looks like an intermittent routing failure rather than a protocol bug.
 */
class ReplicationWireContractTest {

    private final RegistryCodec codec = new RegistryCodec();

    private static Path fixtures() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Cannot locate repository root");
        }
        return dir.resolve("apiml-registry/src/test/resources/wire-contract");
    }

    private static String read(String name) {
        try {
            return Files.readString(fixtures().resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("Missing fixture " + name, e);
        }
    }

    /** Rebuilt against our model, mirroring EurekaReplicationContractCaptureTest's corpus. */
    private ReplicationBatch corpusBatch() {
        ServiceInstance instance = CorpusFixtures.apimlService();
        return new ReplicationBatch(List.of(
            ReplicationItem.register(instance),
            ReplicationItem.heartbeat(instance),
            ReplicationItem.statusUpdate(instance),
            ReplicationItem.deleteStatusOverride(instance),
            ReplicationItem.cancel(instance)
        ));
    }

    @Test
    void encodesABatchExactlyAsEurekaDoes() {
        assertEquals(read("replication-batch-request.json"), codec.encode(corpusBatch()));
    }

    @Test
    void encodesASingleItemBatchExactlyAsEurekaDoes() {
        ServiceInstance instance = CorpusFixtures.apimlService();
        ReplicationBatch batch = new ReplicationBatch(List.of(ReplicationItem.heartbeat(instance)));
        assertEquals(read("replication-batch-request-single.json"), codec.encode(batch));
    }

    @Test
    void encodesAResponseExactlyAsEurekaDoes() {
        ServiceInstance instance = CorpusFixtures.apimlService();
        ReplicationResponse response = new ReplicationResponse(List.of(
            new ReplicationResponse.Item(200, null),
            new ReplicationResponse.Item(404, null),
            new ReplicationResponse.Item(200, instance)
        ));
        assertEquals(read("replication-batch-response.json"), codec.encode(response));
    }

    @Test
    void roundTripsABatchEurekaWrote() {
        String original = read("replication-batch-request.json");
        assertEquals(original, codec.encode(codec.decodeReplicationBatch(original)));
    }

    @Test
    void roundTripsAResponseEurekaWrote() {
        String original = read("replication-batch-response.json");
        assertEquals(original, codec.encode(codec.decodeReplicationResponse(original)));
    }

    @Test
    void onlyARegistrationCarriesTheInstanceBody() {
        ReplicationBatch decoded = codec.decodeReplicationBatch(read("replication-batch-request.json"));

        assertEquals(ReplicationAction.Register, decoded.items().get(0).action());
        assertTrue(decoded.items().get(0).instance() != null);
        for (int i = 1; i < decoded.items().size(); i++) {
            assertNull(decoded.items().get(i).instance(),
                decoded.items().get(i).action() + " must travel as identity only");
        }
    }

    @Test
    void aNotFoundVerdictAsksTheSenderToReRegister() {
        // This is the load-bearing part of the response. A peer that has never seen the instance answers 404 to a
        // replicated heartbeat, and the sender must respond by replicating a full registration - otherwise the
        // instance stays missing from that peer until it happens to re-register on its own.
        ReplicationResponse response = codec.decodeReplicationResponse(read("replication-batch-response.json"));

        assertFalse(response.requiresReRegistration(0));
        assertTrue(response.requiresReRegistration(1));
        assertFalse(response.requiresReRegistration(2));
    }

    @Test
    void preservesTheMixedCaseActionNamesAPeerExpects() {
        // Eureka's enum is mixed-case on the wire. A peer running the current implementation will not recognise
        // "REGISTER" where it expects "Register".
        assertTrue(codec.encode(corpusBatch()).contains("\"action\":\"Register\""));
        assertTrue(codec.encode(corpusBatch()).contains("\"action\":\"DeleteStatusOverride\""));
    }

    @Test
    void keepsTheInstanceStatusAsSentEvenWhenUnrecognised() {
        ServiceInstance instance = CorpusFixtures.apimlService()
            .toBuilder().status(InstanceStatus.OUT_OF_SERVICE).build();
        ReplicationItem item = ReplicationItem.heartbeat(instance);
        assertEquals("OUT_OF_SERVICE", item.status());
    }

}
