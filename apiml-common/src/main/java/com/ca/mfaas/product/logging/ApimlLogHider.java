/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;

@Slf4j
public class ApimlLogHider extends TurboFilter {
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (format != null && format.contains("Tomcat initialized")) {
            return FilterReply.DENY;
        }
        if (format != null && format.contains("route 53")) {
            return FilterReply.DENY;
        }


        if (logger != null) {
          //  System.out.println(format);
           // System.out.println(logger.getName());
        }


        return FilterReply.NEUTRAL;
    }
}
