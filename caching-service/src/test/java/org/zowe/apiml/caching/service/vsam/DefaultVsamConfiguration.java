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

import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.zfile.ZFileConstants;

public class DefaultVsamConfiguration {
    public static VsamConfig defaultConfiguration() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
        generalConfig.setMaxDataSize(100);
        VsamConfig vsamConfiguration = new VsamConfig(generalConfig);
        vsamConfiguration.setFileName("//'DATASET.NAME'");
        vsamConfiguration.setRecordLength(512);
        vsamConfiguration.setKeyLength(64);
        vsamConfiguration.setEncoding(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        return vsamConfiguration;
    }
}
