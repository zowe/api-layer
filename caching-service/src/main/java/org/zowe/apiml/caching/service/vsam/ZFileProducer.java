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
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.zfile.*;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZFileProducer {
    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^//'.*'");

    private final VsamConfig vsamConfig;
    private final VsamConfig.VsamOptions options;
    private final ApimlLogger apimlLog;

    @SuppressWarnings({"java:S1130"}) // It's necessary for the zOS implementations.
    public ZFile openZfile() throws VsamRecordException {
        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalStateException("VsamFile does not exist");
        }
        String calledMethod = "getMessage";
        try {
            return ClassOrDefaultProxyUtils.createProxyByConstructor(ZFile.class, "com.ibm.jzos.ZFile",
                ZFileDummyImpl::new,
                new Class[]{String.class, String.class, int.class},
                new Object[]{vsamConfig.getFileName(), options.getOptionsString(), ZFileConstants.FLAG_DISP_SHR + ZFileConstants.FLAG_PDS_ENQ},
                new ClassOrDefaultProxyUtils.ByMethodName<>(
                    "com.ibm.jzos.ZFileException", ZFileException.class,
                    "getFileName", calledMethod, "getErrnoMsg", "getErrno", "getErrno2", "getLastOp", "getAmrcBytes",
                    "getAbendCode", "getAbendRc", "getFeedbackRc", "getFeedbackFtncd", "getFeedbackFdbk"),
                new ClassOrDefaultProxyUtils.ByMethodName<>(
                    "com.ibm.jzos.RcException", RcException.class,
                    calledMethod, "getRc"),
                new ClassOrDefaultProxyUtils.ByMethodName<>(
                    "com.ibm.jzos.EnqueueException", EnqueueException.class,
                    calledMethod, "getRc")
            );
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            apimlLog.log("org.zowe.apiml.cache.noJzosImplementation");

            throw new JzosImplementationException(e);
        } catch (InvocationTargetException e) {
            throw new VsamRecordException("Failed opening of file", e.getTargetException());
        }
    }
}
