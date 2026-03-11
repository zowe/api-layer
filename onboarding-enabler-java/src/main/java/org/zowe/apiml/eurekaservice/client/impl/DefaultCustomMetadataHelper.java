/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.product.zos.ZUtilDummy;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

import java.util.HashMap;
import java.util.Map;

import static org.zowe.apiml.product.zos.ZosSystemInformation.*;

public class DefaultCustomMetadataHelper {

    @Setter(AccessLevel.PROTECTED)
    private org.zowe.apiml.product.zos.ZUtil zUtil;

    public DefaultCustomMetadataHelper() {
        if (isRunningOnZos()) {
            zUtil = ClassOrDefaultProxyUtils.createProxy(org.zowe.apiml.product.zos.ZUtil.class, "com.ibm.jzos.ZUtil", org.zowe.apiml.product.zos.ZUtilDummy::new);
        } else {
            zUtil = new ZUtilDummy();
        }
    }

    protected boolean isRunningOnZos() {
        return "z/OS".equals(System.getProperty(OS_NAME));
    }

    private Map<String, Object> getDefaultCustomMetadata() {
        Map<String, Object> output = new HashMap<>();

        output.put(OS_NAME, System.getProperty(OS_NAME));

        if (isRunningOnZos()) {
            output.put(ZOS_JOB_ID, zUtil.getCurrentJobId());
            output.put(ZOS_JOB_NAME, zUtil.getCurrentJobname());
            output.put(ZOS_USER_ID, zUtil.getCurrentUser());
            output.put(ZOS_PID, zUtil.getPid());
            output.put(ZOS_SYSNAME, zUtil.substituteSystemSymbols("&SYSNAME."));
            output.put(ZOS_SYSCLONE, zUtil.substituteSystemSymbols("&SYSCLONE."));
            output.put(ZOS_SYSPLEX, zUtil.substituteSystemSymbols("&SYSPLEX."));
        }

        return output;
    }

    public void update(ApiMediationServiceConfig config) {
        if (config.getCustomMetadata() == null) {
            config.setCustomMetadata(new HashMap<>());
        }

        update(config.getCustomMetadata());
    }

    public void update(Map<String, Object> customMetadata) {
        Map<String, Object> defaultMetadata = getDefaultCustomMetadata();
        for (Map.Entry<String, Object> entry : defaultMetadata.entrySet()) {
            customMetadata.putIfAbsent(entry.getKey(), entry.getValue());
        }

    }

}
