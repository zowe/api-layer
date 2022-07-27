/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;

public class VsamFileProducer {
    public VsamFile newVsamFile(VsamConfig config, VsamConfig.VsamOptions options, ApimlLogger apimlLogger) {
        return new VsamFile(config, options, apimlLogger);
    }
}
