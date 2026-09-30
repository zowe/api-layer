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

import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.converters.wrappers.EncoderWrapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * Phase 0 of the Eureka replacement: pin the wire format that the replacement registry has to reproduce.
 * <p>
 * The Eureka JSON and XML representations are not specified anywhere - they are whatever Netflix's Jackson
 * mixins happen to emit. This test drives those codecs over a fixed corpus and compares the output against
 * committed golden files, so the format becomes an asserted contract instead of tribal knowledge.
 * <p>
 * Two modes:
 * <ul>
 *     <li>default - verify. Any drift between the committed fixtures and what Eureka now emits fails the build.
 *         While Eureka is still on the classpath this catches an upgrade changing the format under us.</li>
 *     <li>{@code -Dwire.contract.regenerate=true} - rewrite the fixtures. Review the diff by hand; a change here
 *         is a change to a published contract.</li>
 * </ul>
 * Once the replacement registry exists, its own codec runs against these same files.
 */
class EurekaWireContractCaptureTest {

    private static final String REGENERATE_PROPERTY = "wire.contract.regenerate";

    /** Codec name -> file extension. These four are what the server actually offers. */
    private static final Map<String, String> CODECS = new LinkedHashMap<>();

    static {
        CODECS.put(CodecWrappers.getCodecName(CodecWrappers.JacksonJson.class), "json");
        CODECS.put(CodecWrappers.getCodecName(CodecWrappers.JacksonJsonMini.class), "mini.json");
        CODECS.put(CodecWrappers.getCodecName(CodecWrappers.JacksonXml.class), "xml");
        CODECS.put(CodecWrappers.getCodecName(CodecWrappers.JacksonXmlMini.class), "mini.xml");
    }

    private static Path fixtureDirectory() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Cannot locate the repository root from " + Paths.get("").toAbsolutePath());
        }
        return dir.resolve("apiml-registry/src/test/resources/wire-contract");
    }

    @TestFactory
    List<DynamicTest> eurekaWireFormatMatchesCommittedFixtures() throws IOException {
        boolean regenerate = Boolean.getBoolean(REGENERATE_PROPERTY);
        Path directory = fixtureDirectory();
        if (regenerate) {
            Files.createDirectories(directory);
        }

        List<DynamicTest> tests = new ArrayList<>();
        for (Map.Entry<String, Object> testCase : WireContractCorpus.allCases().entrySet()) {
            for (Map.Entry<String, String> codec : CODECS.entrySet()) {
                Path fixture = directory.resolve(testCase.getKey() + "." + codec.getValue());
                tests.add(DynamicTest.dynamicTest(
                    testCase.getKey() + " [" + codec.getKey() + "]",
                    () -> checkOrWrite(codec.getKey(), testCase.getValue(), fixture, regenerate)
                ));
            }
        }
        return tests;
    }

    private void checkOrWrite(String codecName, Object value, Path fixture, boolean regenerate) throws IOException {
        EncoderWrapper encoder = CodecWrappers.getEncoder(codecName);
        String encoded = encoder.encode(value);

        if (regenerate) {
            Files.writeString(fixture, encoded);
            return;
        }

        if (!Files.exists(fixture)) {
            fail("Missing wire-contract fixture " + fixture.getFileName()
                + ". Run: ./gradlew :discovery-service:test --tests '*EurekaWireContractCaptureTest*' -D"
                + REGENERATE_PROPERTY + "=true");
        }

        assertEquals(
            Files.readString(fixture),
            encoded,
            () -> "Eureka's wire format for " + fixture.getFileName() + " no longer matches the committed fixture. "
                + "This is a change to a contract that deployed enablers depend on - do not regenerate without "
                + "understanding why it moved."
        );
    }

    /** Convenience for reading a fixture from the replacement registry's own tests. */
    static String readFixture(String name) {
        try {
            return Files.readString(fixtureDirectory().resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
