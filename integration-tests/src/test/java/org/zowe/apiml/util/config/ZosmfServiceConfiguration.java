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

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ZosmfServiceConfiguration extends ServiceConfiguration {

    private String serviceId;
    private String contextRoot;

    ZosmfServiceConfiguration(String scheme, String host, String port, String serviceId, String contextRoot) {
        super(scheme, null, host, port, 1);
        this.serviceId = serviceId;
        this.contextRoot = contextRoot;
    }

    @Override
    public boolean isStaticallyRegistred() {
        return true;
    }

}
