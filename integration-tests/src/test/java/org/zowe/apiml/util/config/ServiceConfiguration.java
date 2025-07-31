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

import org.apache.commons.lang3.StringUtils;

public interface ServiceConfiguration {

    String getScheme();
    String getHost();
    int getPort();

    default int getInstances() {
        var host = getHost();
        if (StringUtils.isBlank(host)) {
            return 0;
        }
        return getHost().split(",").length;
    }

    String getServiceId();

}
