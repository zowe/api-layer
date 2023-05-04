/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * Model for setting mock zss service to respond with an error state.
 * Note: Same model is used in the integration-tests module from where
 * the requests are sent.
 */
package org.zowe.apiml.zss.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZssResponse {
    private Integer statusCode;
    private ZssError zssError;

    public enum ZssError {
        MAPPING_NOT_AUTHORIZED,
        MAPPING_EMPTY_INPUT,
        MAPPING_OTHER,
    }
}
