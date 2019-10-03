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

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApimlIntegrationFailureDetector extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (shouldExit(level, t)) {
            System.exit(1);
        }

        if ((logger != null) && logger.getName().contains("com.netflix")
            && (logger.getName().contains("DiscoveryClient")
            || logger.getName().contains("RedirectingEurekaHttpClient"))) {
            if (logger.getLevel() == Level.ERROR) {
                String message = ExceptionUtils.getMessage(t);
                if (message == null) {
                    message = ExceptionUtils.getRootCauseMessage(t);
                }
                if ((message != null) && !message.isEmpty()) {
                 //   log.error(CommonsErrorService.get().getReadableMessage("org.zowe.commons.apiml.unableToRegister", message));
                    logOriginalError(t);
                }
            }
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    boolean shouldExit(Level level, Throwable t) {
        if (level.isGreaterOrEqual(Level.ERROR)) {
            if (ExceptionUtils.indexOfType(t, SSLPeerUnverifiedException.class) >= 0) {
                for (String s : ExceptionUtils.getStackFrames(t)) {
                    if ((s.indexOf(".ApiMediationClient") >= 0)
                        || (s.indexOf("com.netflix.discovery.DiscoveryClient") > 0)) {
                      //  log.error(CommonsErrorService.get().getReadableMessage("org.zowe.commons.apiml.apimlCertificateNotTrusted",
                        //    t.getMessage()));
                        logOriginalError(t);
//                        if (SpringContext.getApplicationContext() == null) {
//                            return true;
//                        }
                    }
                }
            } else if (ExceptionUtils.indexOfType(t, SSLHandshakeException.class) >= 0) {
                for (String s : ExceptionUtils.getStackFrames(t)) {
                    if ((s.indexOf(".ApiMediationClient") >= 0)
                        || (s.indexOf("com.netflix.discovery.DiscoveryClient") > 0)) {
                       // log.error(CommonsErrorService.get().getReadableMessage("org.zowe.commons.apiml.serviceCertificateNotTrusted",
                         //   t.getMessage()));
                        logOriginalError(t);
//                        if (SpringContext.getApplicationContext() == null) {
//                            return true;
//                        }
                    }
                }
            }
        }

        return false;
    }

    private void logOriginalError(Throwable t) {
        log.debug("Original error: {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

}
