/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.model;

/**
 * A port together with whether it is enabled.
 * <p>
 * On the wire this is not a plain number. JSON encodes it as {@code {"$": 10010, "@enabled": "true"}} and XML as
 * {@code <port enabled="true">10010</port>} - note that {@code enabled} is a <em>string</em> in JSON, not a
 * boolean. Both shapes are pinned by the wire-contract fixtures.
 *
 * @param port    the port number
 * @param enabled whether traffic on this port is accepted
 */
public record PortInfo(int port, boolean enabled) {

    /** Eureka's historical defaults, emitted when a service does not declare a port. */
    public static final int DEFAULT_PORT = 7001;
    public static final int DEFAULT_SECURE_PORT = 7002;

    public static PortInfo unsecure(int port, boolean enabled) {
        return new PortInfo(port, enabled);
    }

    public static PortInfo disabledDefault(int defaultPort) {
        return new PortInfo(defaultPort, false);
    }

}
