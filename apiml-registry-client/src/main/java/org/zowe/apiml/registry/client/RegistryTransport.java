/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * How a client reaches a registry.
 * <p>
 * Two implementations matter, and the difference is the point of this interface. A remote service talks HTTP to
 * the Discovery Service. Inside the modulith, though, the registry is a bean in the same JVM, and the Eureka-based
 * arrangement still went out over HTTP to reach it - the process polling {@code /eureka/apps} on its own port.
 * That was wasteful, and worse: the modulith gates {@code /eureka/**} to the internal Discovery port, so the
 * Gateway-port client was being refused and retrying. A local implementation removes the round trip entirely.
 */
public interface RegistryTransport {

    /** The whole registry. */
    Applications fetchApplications() throws RegistryTransportException;

    /**
     * Instances changed since the last fetch.
     *
     * @return the delta, or null when this transport does not support deltas and a full fetch should be used
     */
    Applications fetchDelta() throws RegistryTransportException;

    void register(ServiceInstance instance) throws RegistryTransportException;

    /** @return false when the registry does not know the instance, meaning the caller must re-register */
    boolean renew(String appName, String instanceId) throws RegistryTransportException;

    void cancel(String appName, String instanceId) throws RegistryTransportException;

    void updateStatus(String appName, String instanceId, InstanceStatus status) throws RegistryTransportException;

    /** A transport-level failure, distinct from the registry answering "not found". */
    class RegistryTransportException extends Exception {

        private static final long serialVersionUID = 1L;

        public RegistryTransportException(String message, Throwable cause) {
            super(message, cause);
        }

        public RegistryTransportException(String message) {
            super(message);
        }

    }

}
