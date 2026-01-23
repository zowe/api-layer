/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.zos;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

import java.util.Map;

@Component
public class ZosSystemInformation {

    public static final String ZOS_JOB_ID = "zos.jobid";
    public static final String ZOS_JOB_NAME = "zos.jobname";
    public static final String ZOS_USER_ID = "zos.userid";
    public static final String ZOS_PID = "zos.pid";
    public static final String ZOS_SYSNAME = "zos.sysname";
    public static final String ZOS_SYSCLONE = "zos.sysclone";
    public static final String ZOS_SYSPLEX = "zos.sysplex";
    public static final String OS_NAME = "os.name";

    private ZUtil zUtil;

    public static boolean isRunningOnZos() {
        return "z/OS".equals(System.getProperty(OS_NAME));
    }

    public Map<String, Object> get() {
        return Map.of(
            ZOS_JOB_ID, zUtil.getCurrentJobId(),
            ZOS_JOB_NAME, zUtil.getCurrentJobname(),
            ZOS_USER_ID, zUtil.getCurrentUser(),
            ZOS_PID, zUtil.getPid(),
            ZOS_SYSNAME, zUtil.substituteSystemSymbols("&SYSNAME."),
            ZOS_SYSCLONE, zUtil.substituteSystemSymbols("&SYSCLONE."),
            ZOS_SYSPLEX, zUtil.substituteSystemSymbols("&SYSPLEX.")
        );
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        if (isRunningOnZos()) {
            zUtil = ClassOrDefaultProxyUtils.createProxy(ZUtil.class, "com.ibm.jzos.ZUtil", ZUtilDummy::new);
        } else {
            zUtil = new ZUtilDummy();
        }

    }

}
