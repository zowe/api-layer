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

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.zfile.*;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZFileProducer {
    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^\\/\\/\\'.*'");

    private final VsamConfig vsamConfig;
    private final VsamConfig.VsamOptions options;

    public ZFile openZfile() throws ZFileException {
        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalStateException("VsamFile does not exist");
        }
        return ClassOrDefaultProxyUtils.createProxyByConstructor(ZFile.class, "com.ibm.jzos.ZFile",
            ZFileDummyImpl::new,
            new Class[]{String.class, String.class, int.class},
            new Object[]{vsamConfig.getFileName(), options.getOptionsString(), ZFileConstants.FLAG_DISP_SHR + ZFileConstants.FLAG_PDS_ENQ},
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.ZFileException", ZFileException.class,
                "getFileName", "getMessage", "getErrnoMsg", "getErrno", "getErrno2", "getLastOp", "getAmrcBytes",
                "getAbendCode", "getAbendRc", "getFeedbackRc", "getFeedbackFtncd", "getFeedbackFdbk"),
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.RcException", RcException.class,
                "getMessage", "getRc"),
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.EnqueueException", EnqueueException.class,
                "getMessage", "getRc")
        );
    }
}
