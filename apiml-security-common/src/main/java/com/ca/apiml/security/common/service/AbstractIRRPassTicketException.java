/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstact exception from IRR passticket service. It collect common values about exception
 */
@AllArgsConstructor
@Getter
public abstract class AbstractIRRPassTicketException extends Exception {

    private static final long serialVersionUID = -6233392272992529775L;

    protected final int safRc;
    protected final int racfRsn;
    protected final int racfRc;

    public List<ErrorCode> getErrorCodes() {
        return ErrorCode.getErrorCode(this);
    }

    protected String getMessage(String baseMessage) {
        final StringBuilder sb = new StringBuilder();

        sb.append(baseMessage).append('\n');
        for (ErrorCode ec : getErrorCodes()) {
            sb.append('\t').append(ec.getMessage()).append('\n');
        }

        return sb.toString();
    }

    @AllArgsConstructor
    @Getter
    public enum ErrorCode {

            ERR_0_0_0(0, 0, 0, "The service was successful."),
            ERR_4_0_0(4, 0, 0, "RACF is not installed."),
            ERR_8_8_0(8, 8, 0, "Invalid function code."),
            ERR_8_8_4(8, 8, 4, "Parameter list error."),
            ERR_8_8_8(8, 8, 8, "An internal error was encountered."),
            ERR_8_8_12(8, 8, 12, "A recovery environment could not be established."),
            ERR_8_8_16(8, 8, 16, "Not authorized to use this service."),
            ERR_8_8_20(8, 8, 20, "High order bit was not set to indicate last parameter."),
            ERR_8_12_8(8, 12, 8, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with a 'parameter buffer overflow' return code. This indicates an internal error in IRRSPK00."),
            ERR_8_12_12(8, 12, 12, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'unable to allocate storage' return code. The region size for the Security Server Network Authentication Service started task (SKRBKDC) should be increased."),
            ERR_8_12_16(8, 12, 16, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with a 'local services are not available' return code. This indicates that the Security Server Network Authentication Service started task (SKRBKDC) address space has not been started or is terminating."),
            ERR_8_12_20(8, 12, 20, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'abend in the PC service routine' return code. The symptom record associated with this abend can be found in the logrec data set."),
            ERR_8_12_24(8, 12, 24, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'unable to obtain control lock' return code. This can occur if the task holding the lock is not being dispatched (for example, a dump is in progress)."),
            ERR_8_16_X_1(8, 16, null, "The Security Server Network Authentication Service was not able to successfully extract the client principal name from the supplied Kerberos V5 ticket. X'nnnnnnnn' is the Kerberos return code. Refer to the Security Server Network Authentication Service documentation for more information."),
            ERR_8_16_28(8, 16, 28, "Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide."),
            ERR_8_16_32(8, 16, 32,"PassTicket evaluation failure. Possible reasons include:\n" +
                "- PassTicket to be evaluated is not a successful PassTicket\n" +
                "- The PassTicket to be evaluated was already evaluated before and replay protection is in effect.\n" +
                "- No PTKTDATA profile exists to match the specified application\n" +
                "- An internal error occurred."),
            ERR_8_16_X_2(8, 16, null, "PassTicket evaluation extended failure. X'nnnnnnnn' is the internal reason code for the evaluation failure.")

        ;


        private final int safRc;
        private final int racfRsn;
        private final Integer racfRc;

        private String message;

        public static List<ErrorCode> getErrorCode(AbstractIRRPassTicketException e) {
            final List<ErrorCode> out = new LinkedList<>();

            for (final ErrorCode ec : values()) {
                if (ec.getSafRc() != e.getSafRc()) continue;
                if (ec.getRacfRsn() != e.getRacfRsn()) continue;
                if ((ec.getRacfRc() != null) && (ec.getRacfRc() != e.getRacfRc())) continue;
                out.add(ec);
            }

            return out;
        }

    }

}
