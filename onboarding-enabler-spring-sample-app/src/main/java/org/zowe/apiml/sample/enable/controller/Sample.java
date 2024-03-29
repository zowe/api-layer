/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.sample.enable.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Sample class for returning data
 */
@Data
@AllArgsConstructor
public class Sample {
    private String name;
    private String details;
    private int index;
}
