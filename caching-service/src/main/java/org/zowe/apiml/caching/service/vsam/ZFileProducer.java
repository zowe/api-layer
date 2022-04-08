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
// import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import com.ibm.jzos.*;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZFileProducer {
    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^//'.*'");

    private final VsamConfig vsamConfig;
    private final VsamConfig.VsamOptions options;
    private final ApimlLogger apimlLog;

    @SuppressWarnings({"java:S1130"}) // It's necessary for the zOS implementations.
    public ZFile openZfile() throws VsamRecordException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalStateException("VsamFile does not exist");
        }
        String calledMethod = "getMessage";
        ZFile zfile = new ZFile();
        return zfile;
    }
}
