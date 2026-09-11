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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Phase 1 acceptance gate: this codec must reproduce Eureka's bytes exactly.
 * <p>
 * The fixtures were captured from Eureka 2.0.6 by
 * {@code org.zowe.apiml.discovery.contract.EurekaWireContractCaptureTest}. Nothing here is asserted against my
 * expectation of the format - only against what Eureka actually emitted. A failure means a deployed enabler would
 * see something different from what it sees today.
 */
class RegistryCodecWireContractTest {

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

    private static String fixture(String name) {
        try {
            return Files.readString(fixtures().resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("Missing fixture " + name, e);
        }
    }

    private record Case(String name, Object value) {
    }

    /** Mirrors WireContractCorpus, rebuilt against our own model. */
    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("instance-apiml-service", CorpusFixtures.apimlService()));
        cases.add(new Case("instance-static-service", CorpusFixtures.staticService()));
        cases.add(new Case("instance-minimal", CorpusFixtures.minimalInstance()));
        cases.add(new Case("instance-overridden-down", CorpusFixtures.overriddenDownInstance()));
        cases.add(new Case("instance-all-fields", CorpusFixtures.allFieldsInstance()));
        cases.add(new Case("application-single", CorpusFixtures.singleApplication()));
        cases.add(new Case("applications-registry", CorpusFixtures.registry()));
        cases.add(new Case("applications-delta", CorpusFixtures.delta()));
        cases.add(new Case("applications-empty", CorpusFixtures.emptyRegistry()));
        cases.add(new Case("applications-mixed-status", CorpusFixtures.mixedStatusRegistry()));
        return cases;
    }

    private static final Map<WireFormat, String> SUFFIXES = Map.of(
        WireFormat.JSON_FULL, "json",
        WireFormat.JSON_COMPACT, "mini.json",
        WireFormat.XML_FULL, "xml",
        WireFormat.XML_COMPACT, "mini.xml"
    );

    @TestFactory
    List<DynamicTest> reproducesEurekaBytes() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Case testCase : cases()) {
            for (WireFormat format : WireFormat.values()) {
                String file = testCase.name() + "." + SUFFIXES.get(format);
                tests.add(DynamicTest.dynamicTest(file, () ->
                    assertEquals(fixture(file), encode(testCase.value(), format),
                        "Encoded output differs from what Eureka emits for " + file)
                ));
            }
        }
        return tests;
    }

    private String encode(Object value, WireFormat format) {
        if (value instanceof ServiceInstance instance) {
            return codec.encode(instance, format);
        }
        if (value instanceof Application application) {
            return codec.encode(application, format);
        }
        return codec.encode((Applications) value, format);
    }

    @Test
    void appsHashCodeOrdersStatusesAlphabeticallyLikeEureka() {
        // Guards the ordering bug that an all-UP registry cannot catch: Eureka keys its count map by status NAME
        // in a TreeMap, so DOWN sorts before UP even though the enum declares UP first.
        Applications mixed = CorpusFixtures.mixedStatusRegistry();
        assertEquals(
            "DOWN_1_OUT_OF_SERVICE_1_STARTING_1_UP_2_",
            Applications.computeHashCode(mixed.applications())
        );
    }

}
