/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.contract;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.converters.wrappers.EncoderWrapper;
import com.netflix.eureka.cluster.protocol.ReplicationInstance;
import com.netflix.eureka.cluster.protocol.ReplicationInstanceResponse;
import com.netflix.eureka.cluster.protocol.ReplicationList;
import com.netflix.eureka.cluster.protocol.ReplicationListResponse;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Phase 0, second half: pin the peer-replication batch protocol.
 * <p>
 * This has to be captured while Eureka is still on the classpath, because Eureka is what defines the format -
 * once Phase 3 removes it there is nothing left to compare against. HA across LPARs rides on this protocol, so a
 * mismatch means one Discovery Service silently fails to learn about services registered on the other.
 * <p>
 * Regenerate with {@code -Dwire.contract.regenerate=true}, same as the registry fixtures.
 */
class EurekaReplicationContractCaptureTest {

    private static final String REGENERATE_PROPERTY = "wire.contract.regenerate";

    /**
     * The full JSON codec - the same one the replication client uses.
     * <p>
     * This is deliberately not a plain ObjectMapper. The replication DTOs do carry {@code @JsonCreator} /
     * {@code @JsonProperty}, so a plain mapper appears to work, but it serialises the nested InstanceInfo without
     * Eureka's mixins: the port comes out as {@code null} and the field order differs. What actually goes on the
     * wire is whatever {@code DiscoveryJerseyProvider(fullJsonCodec, fullJsonCodec)} produces - see
     * RefreshablePeerEurekaNodes.createReplicationClient - so the fixtures have to be captured through that codec
     * or they would pin a format no peer ever sends.
     */
    private final EncoderWrapper encoder = CodecWrappers.getEncoder(CodecWrappers.JacksonJson.class);

    private static Path fixtureDirectory() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Cannot locate the repository root");
        }
        return dir.resolve("apiml-registry/src/test/resources/wire-contract");
    }

    private static ReplicationInstance replicationInstance(Action action, InstanceInfo info, boolean withBody) {
        return ReplicationInstance.replicationInstance()
            .withAppName(info.getAppName())
            .withId(info.getId())
            .withLastDirtyTimestamp(info.getLastDirtyTimestamp())
            .withStatus(info.getStatus() == null ? null : info.getStatus().name())
            .withOverriddenStatus(info.getOverriddenStatus() == null ? null : info.getOverriddenStatus().name())
            .withInstanceInfo(withBody ? info : null)
            .withAction(action)
            .build();
    }

    private Map<String, Object> cases() {
        InstanceInfo info = WireContractCorpus.apimlService();
        Map<String, Object> cases = new LinkedHashMap<>();

        // A register carries the whole instance; a heartbeat and a cancel carry only the identity, which is the
        // whole point of the batch protocol being cheap.
        ReplicationList batch = new ReplicationList();
        batch.addReplicationInstance(replicationInstance(Action.Register, info, true));
        batch.addReplicationInstance(replicationInstance(Action.Heartbeat, info, false));
        batch.addReplicationInstance(replicationInstance(Action.StatusUpdate, info, false));
        batch.addReplicationInstance(replicationInstance(Action.DeleteStatusOverride, info, false));
        batch.addReplicationInstance(replicationInstance(Action.Cancel, info, false));
        cases.put("replication-batch-request", batch);

        cases.put("replication-batch-request-single",
            new ReplicationList(replicationInstance(Action.Heartbeat, info, false)));

        // 200 for an accepted item; 404 on a heartbeat is how a peer says "I do not know this instance, send me
        // the full registration" - the response entity is then the peer's own copy, if it has one.
        ReplicationListResponse response = new ReplicationListResponse();
        response.addResponse(new ReplicationInstanceResponse(200, null));
        response.addResponse(new ReplicationInstanceResponse(404, null));
        response.addResponse(new ReplicationInstanceResponse(200, info));
        cases.put("replication-batch-response", response);

        return cases;
    }

    @TestFactory
    List<DynamicTest> replicationProtocolMatchesCommittedFixtures() throws IOException {
        boolean regenerate = Boolean.getBoolean(REGENERATE_PROPERTY);
        Path directory = fixtureDirectory();
        if (regenerate) {
            Files.createDirectories(directory);
        }

        List<DynamicTest> tests = new ArrayList<>();
        for (Map.Entry<String, Object> testCase : cases().entrySet()) {
            Path fixture = directory.resolve(testCase.getKey() + ".json");
            tests.add(DynamicTest.dynamicTest(testCase.getKey(), () -> {
                String encoded = encoder.encode(testCase.getValue());
                if (regenerate) {
                    Files.writeString(fixture, encoded);
                    return;
                }
                if (!Files.exists(fixture)) {
                    fail("Missing replication fixture " + fixture.getFileName() + "; regenerate with -D"
                        + REGENERATE_PROPERTY + "=true");
                }
                assertEquals(Files.readString(fixture), encoded,
                    "The peer-replication format changed. HA replication between LPARs depends on this.");
            }));
        }
        return tests;
    }

}
