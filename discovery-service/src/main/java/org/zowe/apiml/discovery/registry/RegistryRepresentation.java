/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import org.springframework.http.MediaType;
import org.zowe.apiml.registry.codec.WireFormat;

/**
 * Chooses the representation for a registry response.
 * <p>
 * The rules are measured, not assumed - see {@code wire-contract/http-contract.json}. The one that matters:
 * <b>XML is the default</b>. An absent {@code Accept} header and {@code Accept: *}{@code /*} both yield XML, and
 * only an explicit {@code application/json} yields JSON. Defaulting to JSON here - the obvious modern choice -
 * would silently change the response body for every client that does not ask specifically, which includes naive
 * HTTP clients and customer scripts.
 */
public final class RegistryRepresentation {

    /** Eureka's own header for requesting the reduced projection. */
    public static final String EUREKA_ACCEPT_HEADER = "X-Eureka-Accept";
    private static final String COMPACT = "compact";

    private RegistryRepresentation() {
    }

    public static WireFormat resolve(String acceptHeader, String eurekaAcceptHeader) {
        boolean compact = eurekaAcceptHeader != null && COMPACT.equalsIgnoreCase(eurekaAcceptHeader.trim());
        return WireFormat.of(syntaxFor(acceptHeader), compact);
    }

    private static WireFormat.Syntax syntaxFor(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return WireFormat.Syntax.XML;
        }
        // Only an explicit JSON request gets JSON. A wildcard is not explicit.
        return acceptHeader.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)
            ? WireFormat.Syntax.JSON
            : WireFormat.Syntax.XML;
    }

    public static MediaType contentTypeFor(WireFormat format) {
        return format.syntax() == WireFormat.Syntax.JSON
            ? MediaType.APPLICATION_JSON
            : MediaType.APPLICATION_XML;
    }

}
