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

/**
 * Sends a batch to one peer.
 * <p>
 * Kept as an interface so the batching, retry and re-registration logic can be tested without a peer, a socket or
 * a TLS handshake. The real implementation lives in the Spring layer, where the AT-TLS and hostname-verification
 * behaviour that the current {@code RefreshablePeerEurekaNodes} carries has to be reproduced.
 */
public interface ReplicationTransport {

    /**
     * @param peerUrl base URL of the peer, e.g. {@code https://host:10011/eureka/}
     * @param batch   the changes to send
     * @return the peer's per-item verdicts
     * @throws ReplicationTransportException when the peer could not be reached or answered unusably
     */
    ReplicationResponse send(String peerUrl, ReplicationBatch batch) throws ReplicationTransportException;

    /** A transport-level failure, as distinct from a peer rejecting an individual item. */
    class ReplicationTransportException extends Exception {

        private static final long serialVersionUID = 1L;

        public ReplicationTransportException(String message, Throwable cause) {
            super(message, cause);
        }

        public ReplicationTransportException(String message) {
            super(message);
        }

    }

}
