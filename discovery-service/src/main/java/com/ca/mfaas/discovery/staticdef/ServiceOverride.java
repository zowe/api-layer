/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.staticdef;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceOverride extends  Service {

    private Mode mode;

    public enum Mode {

        /**
         * default mode
         *
         * This mode means, update all filled values in original, if original value is missing. If this value is
         * filled do nothing
         */
        UPDATE,

        /**
         * This mode rewrite all filled values. It different original values are filled or not.
         */
        FORCE_UPDATE

    }

}
