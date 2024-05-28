/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.webfinger;

import lombok.Data;

import java.util.List;

@Data
public class WebFingerProperties {

    private List<WebFingerConfig> webFinger;

    @Data
    static class WebFingerConfig {
        private String clientId;
        private String wellKnown;
    }
}
