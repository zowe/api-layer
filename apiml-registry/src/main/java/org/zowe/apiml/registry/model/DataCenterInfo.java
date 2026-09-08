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
 * Where an instance runs.
 * <p>
 * APIML only ever uses {@link Name#MyOwn}; the AWS variants that Eureka supported are deliberately not modelled
 * (see TASK-Discovery-Native-Registry.md section 3). The type discriminator emitted on the wire is nonetheless a
 * Netflix class name - see {@code org.zowe.apiml.registry.codec.WireConstants} - because deployed clients match
 * on that exact string.
 *
 * @param name the data centre name
 */
public record DataCenterInfo(Name name) {

    public enum Name {
        MyOwn,
        Amazon,
        Netflix
    }

    public static final DataCenterInfo MY_OWN = new DataCenterInfo(Name.MyOwn);

    public static DataCenterInfo fromWire(String value) {
        if (value == null) {
            return MY_OWN;
        }
        for (Name candidate : Name.values()) {
            if (candidate.name().equalsIgnoreCase(value.trim())) {
                return new DataCenterInfo(candidate);
            }
        }
        return MY_OWN;
    }

}
