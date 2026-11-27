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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Holds the network address of the peer that sent the Eureka request processed by the current thread. The value is
 * the address of the TCP connection, not a value declared by the client, so it can be used to verify that
 * a registrant really runs on the host it declares in its registration.
 * <p>
 * No value is set for registrations created internally (e.g. static definitions or the modulith's own services).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegistrationRequestContext {

    private static final ThreadLocal<InetAddress> PEER_ADDRESS = new ThreadLocal<>();

    public static Optional<InetAddress> getPeerAddress() {
        return Optional.ofNullable(PEER_ADDRESS.get());
    }

    public static void setPeerAddress(InetAddress peerAddress) {
        PEER_ADDRESS.set(peerAddress);
    }

    public static void clear() {
        PEER_ADDRESS.remove();
    }

    /**
     * Runs the action with the peer address bound to the current thread
     *
     * @param peerAddress address of the TCP peer which sent the request
     * @param action      action processing the request
     * @return result of the action
     */
    public static <T> T callWith(InetAddress peerAddress, Supplier<T> action) {
        setPeerAddress(peerAddress);
        try {
            return action.get();
        } finally {
            clear();
        }
    }

}
