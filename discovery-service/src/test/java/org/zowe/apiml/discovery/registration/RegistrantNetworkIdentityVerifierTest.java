/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registration;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrantNetworkIdentityVerifierTest {

    private static final String PEER_URL = "https://127.0.0.1:10011/eureka/";

    private RegistrantNetworkIdentityVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new RegistrantNetworkIdentityVerifier();
    }

    private static InstanceInfo instance(String hostname) {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(hostname + ":service:10012")
            .setAppName("service")
            .setHostName(hostname)
            .build();
    }

    private static InetAddress address(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip);
    }

    private void verify(String peerIp, InstanceInfo info, boolean isReplication) throws UnknownHostException {
        RegistrationRequestContext.callWith(address(peerIp), () -> {
            verifier.verify(info, isReplication, List.of(PEER_URL));
            return null;
        });
    }

    @Nested
    class GivenNoPeerAddress {

        @Test
        void whenVerify_thenSkipValidation() {
            assertDoesNotThrow(() -> verifier.verify(instance("10.1.1.1"), false, List.of()));
            assertDoesNotThrow(() -> verifier.verify(instance("10.1.1.1"), true, List.of()));
        }

    }

    @Nested
    class GivenDirectRegistration {

        @Test
        void whenHostnameResolvesToPeerAddress_thenAccept() {
            assertDoesNotThrow(() -> verify("10.1.1.1", instance("10.1.1.1"), false));
        }

        @Test
        void whenHostnameDoesNotResolveToPeerAddress_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> verify("10.1.1.2", instance("10.1.1.1"), false));
        }

        @Test
        void whenRemotePeerDeclaresLocalHost_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> verify("10.1.1.2", instance("localhost"), false));
        }

        @Test
        void whenLocalPeerDeclaresAnotherLocalAddress_thenAccept() {
            assertDoesNotThrow(() -> verify("127.0.0.1", instance("localhost"), false));
            assertDoesNotThrow(() -> verify("127.0.0.2", instance("127.0.0.1"), false));
        }

        @Test
        void whenHostnameIsBlank_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> verify("127.0.0.1", instance(""), false));
        }

        @Test
        void whenHostnameCannotBeResolved_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> verify("127.0.0.1", instance("unknown.host.invalid"), false));
        }

        @Test
        void whenValidationIsDisabled_thenAccept() {
            ReflectionTestUtils.setField(verifier, "validationDisabled", true);
            assertDoesNotThrow(() -> verify("10.1.1.2", instance("10.1.1.1"), false));
        }

    }

    @Nested
    class GivenReplication {

        @Test
        void whenSentByPeer_thenAccept() {
            assertDoesNotThrow(() -> verify("127.0.0.1", instance("10.1.1.1"), true));
        }

        @Test
        void whenNotSentByPeer_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> verify("10.1.1.2", instance("10.1.1.2"), true));
        }

        @Test
        void whenPeerUrlIsInvalid_thenReject() {
            assertThrows(RegistrantNetworkIdentityException.class, () -> RegistrationRequestContext.callWith(address("127.0.0.1"), () -> {
                verifier.verify(instance("127.0.0.1"), true, List.of("not a url", "https:///path"));
                return null;
            }));
        }

    }

    @Nested
    class LocalAddress {

        @Test
        void whenLoopbackOrAny_thenLocal() throws UnknownHostException {
            assertTrue(verifier.isLocalAddress(address("127.0.0.1")));
            assertTrue(verifier.isLocalAddress(address("0.0.0.0")));
        }

        @Test
        void whenRemote_thenNotLocal() throws UnknownHostException {
            assertFalse(verifier.isLocalAddress(address("192.0.2.1")));
        }

    }

}
