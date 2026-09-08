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
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decoding, tested against Eureka's own output rather than against our encoder.
 * <p>
 * Reading a fixture Eureka wrote, then re-encoding it and requiring the same bytes back, exercises both directions
 * at once and cannot be satisfied by a matching pair of mistakes in our own encoder and decoder.
 */
class RegistryCodecRoundTripTest {

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

    @TestFactory
    List<DynamicTest> decodesEurekaOutputAndReEncodesIdentically() {
        // Only the full projections round-trip: the compact ones deliberately discard fields, so re-encoding a
        // compact payload as compact is the only meaningful comparison there, and it is covered below.
        List<String> instanceCases = List.of(
            "instance-apiml-service", "instance-static-service", "instance-minimal",
            "instance-overridden-down", "instance-all-fields"
        );
        List<DynamicTest> tests = new ArrayList<>();

        for (String name : instanceCases) {
            for (String ext : List.of("json", "xml")) {
                WireFormat format = "json".equals(ext) ? WireFormat.JSON_FULL : WireFormat.XML_FULL;
                String file = name + "." + ext;
                tests.add(DynamicTest.dynamicTest(file, () -> {
                    String original = read(file);
                    ServiceInstance decoded = codec.decodeInstance(original);
                    assertEquals(original, codec.encode(decoded, format),
                        "Round-trip of " + file + " lost or altered a field");
                }));
            }
        }

        for (String name : List.of("applications-registry", "applications-delta", "applications-mixed-status")) {
            for (String ext : List.of("json", "xml")) {
                WireFormat format = "json".equals(ext) ? WireFormat.JSON_FULL : WireFormat.XML_FULL;
                String file = name + "." + ext;
                tests.add(DynamicTest.dynamicTest(file, () -> {
                    String original = read(file);
                    assertEquals(original, codec.encode(codec.decodeApplications(original), format),
                        "Round-trip of " + file + " lost or altered a field");
                }));
            }
        }

        return tests;
    }

    @Test
    void readsAPortSentAsABareNumberByOlderClients() {
        // Not every client wraps the port. A declared port with no enabled flag is treated as enabled, because
        // that is plainly what the sender meant; rejecting it would drop services that register successfully today.
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S","hostName":"h","port":8080}}""");
        assertEquals(8080, instance.port().port());
        assertTrue(instance.port().enabled());
    }

    @Test
    void treatsAnAbsentPortAsTheDisabledDefault() {
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S","hostName":"h"}}""");
        assertEquals(PortInfo.DEFAULT_PORT, instance.port().port());
        assertFalse(instance.port().enabled());
        assertEquals(PortInfo.DEFAULT_SECURE_PORT, instance.securePort().port());
        assertFalse(instance.securePort().enabled());
    }

    @Test
    void doesNotMistakeTheEmptyMetadataMarkerForMetadata() {
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S",\
            "metadata":{"@class":"java.util.Collections$EmptyMap"}}}""");
        assertTrue(instance.metadata().isEmpty());
    }

    @Test
    void recognisesAPermanentLeaseFromItsDuration() {
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S","leaseInfo":{"durationInSecs":2147483}}}""");
        assertEquals(Lease.Kind.PERMANENT, instance.lease().kind());
        assertFalse(instance.lease().expired(Long.MAX_VALUE));
    }

    @Test
    void mapsAnUnrecognisedStatusToUnknownRatherThanFailing() {
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S","status":"SOMETHING_NEW"}}""");
        assertEquals(InstanceStatus.UNKNOWN, instance.status());
    }

    @Test
    void treatsAnAbsentOverrideAsUnknown() {
        ServiceInstance instance = codec.decodeInstance("""
            {"instance":{"instanceId":"h:s:1","app":"S","status":"UP"}}""");
        assertEquals(InstanceStatus.UNKNOWN, instance.overriddenStatus());
        assertEquals(InstanceStatus.UP, instance.effectiveStatus());
    }

    @Test
    void upperCasesAnAppNameSetProgrammaticallyButNotOneReadOffTheWire() {
        // Netflix's InstanceInfo.Builder.setAppName upper-cased (Locale.ROOT) while setAppNameForDeser did not,
        // and both behaviours matter. The static-definition processor passes a lower-case service id and expects
        // the upper-case form to be stored and published, because `app` is upper-case on the wire. The decoder
        // must instead round-trip exactly what arrived, or decode(encode(x)) stops being identity.
        //
        // The wire-contract corpus cannot catch this: every case in it passes an already-upper-case name.
        ServiceInstance built = ServiceInstance.builder()
            .instanceId("localhost:mixedcase:1")
            .appName("MixedCase")
            .build();
        assertEquals("MIXEDCASE", built.appName());

        ServiceInstance decoded = codec.decodeInstance("""
            {"instance":{"instanceId":"localhost:mixedcase:1","app":"MixedCase"}}""");
        assertEquals("MixedCase", decoded.appName(), "a decoder must not normalise what the sender sent");

        // And toBuilder must not re-normalise a decoded instance on the way through.
        assertEquals("MixedCase", decoded.toBuilder().hostName("h").build().appName());
    }

    @Test
    void upperCasesAppGroupNameTheSameWay() {
        assertEquals("ZOWE_GROUP", ServiceInstance.builder()
            .instanceId("h:s:1").appName("S").appGroupName("zowe_group").build().appGroupName());
    }

    @Test
    void rejectsAnExternalEntityInsteadOfResolvingIt() {
        // Registration payloads are untrusted, and Zowe documents POSTing XML to /eureka/apps directly, so the
        // reader must not resolve entities. DTD support is disabled outright, so this is refused at parse time
        // rather than silently returning the contents of /etc/passwd.
        String xxe = """
            <?xml version="1.0"?>
            <!DOCTYPE instance [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <instance><instanceId>&xxe;</instanceId><app>S</app></instance>""";
        assertThrows(IllegalArgumentException.class, () -> codec.decodeInstance(xxe));
    }

}
