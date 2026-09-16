/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

public interface ServiceConfiguration {

    String getScheme();

    String getHost();

    int getPort();

    String getServiceId();

    default boolean isStaticallyRegistred() {
        return false;
    }

    default String getServletContext() {
        return "/";
    }

    default boolean isBasicAuthenticationSupported() {
        return true;
    }

    /**
     * Comma-separated ports, positionally paired with getHost()'s comma-separated hosts, that
     * ApiMediationLayerStartupChecker should actually connect on - as opposed to getPort(), which
     * stays the port every instance's own identity/self-registration is reported under in eureka.
     * They're normally the same (real container-to-container traffic shares one real port
     * regardless of hostname), but differ when a same-port secondary instance is published to a
     * different host port for host-based test execution (see docker/integration-tests/README.md).
     * Null/blank (the default) means every host uses getPort().
     */
    default String getConnectPorts() {
        return null;
    }

}
