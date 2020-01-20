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
import org.springframework.http.HttpStatus;

/**
 * Abstract exception from IRR passticket service. It collect common values about exception
 */
@AllArgsConstructor
@Getter
public abstract class AbstractIRRPassTicketException extends Exception {

    private static final long serialVersionUID = -6233392272992529775L;

    protected final int safRc;
    protected final int racfRc;
    protected final int racfRsn;

    public AbstractIRRPassTicketException(ErrorCode errorCode) {
        this.safRc = errorCode.getSafRc();
        this.racfRc = errorCode.getRacfRc();
        this.racfRsn = errorCode.getRacfRsn();
    }

    public ErrorCode getErrorCode() {
        return ErrorCode.getErrorCode(this);
    }

    protected String getMessage(String baseMessage) {
        return baseMessage + ' ' + getErrorCode().getMessage();
    }

    public HttpStatus getHttpStatus() {
        return getErrorCode().getHttpStatus();
    }

    @AllArgsConstructor
    @Getter
    public enum ErrorCode {

        ERR_0_0_0(0, 0, 0, HttpStatus.INTERNAL_SERVER_ERROR, "The service was successful."),
        ERR_4_0_0(4, 0, 0, HttpStatus.INTERNAL_SERVER_ERROR, "RACF is not installed."),
        ERR_8_8_0(8, 8, 0, HttpStatus.INTERNAL_SERVER_ERROR, "Invalid function code."),
        ERR_8_8_4(8, 8, 4, HttpStatus.INTERNAL_SERVER_ERROR, "Parameter list error."),
        ERR_8_8_8(8, 8, 8, HttpStatus.INTERNAL_SERVER_ERROR, "An internal error was encountered."),
        ERR_8_8_12(8, 8, 12, HttpStatus.INTERNAL_SERVER_ERROR, "A recovery environment could not be established."),
        ERR_8_8_16(8, 8, 16, HttpStatus.INTERNAL_SERVER_ERROR, "Not authorized to use this service."),
        ERR_8_8_20(8, 8, 20, HttpStatus.INTERNAL_SERVER_ERROR, "High order bit was not set to indicate last parameter."),
        ERR_8_12_8(8, 12, 8, HttpStatus.INTERNAL_SERVER_ERROR, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with a 'parameter buffer overflow' return code. This indicates an internal error in IRRSPK00."),
        ERR_8_12_12(8, 12, 12, HttpStatus.INTERNAL_SERVER_ERROR, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'unable to allocate storage' return code. The region size for the Security Server Network Authentication Service started task (SKRBKDC) should be increased."),
        ERR_8_12_16(8, 12, 16, HttpStatus.INTERNAL_SERVER_ERROR, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with a 'local services are not available' return code. This indicates that the Security Server Network Authentication Service started task (SKRBKDC) address space has not been started or is terminating."),
        ERR_8_12_20(8, 12, 20, HttpStatus.INTERNAL_SERVER_ERROR, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'abend in the PC service routine' return code. The symptom record associated with this abend can be found in the logrec data set."),
        ERR_8_12_24(8, 12, 24, HttpStatus.INTERNAL_SERVER_ERROR, "Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'unable to obtain control lock' return code. This can occur if the task holding the lock is not being dispatched (for example, a dump is in progress)."),
        ERR_8_16_28(8, 16, 28, HttpStatus.BAD_REQUEST, "Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide."),
        ERR_8_16_32(8, 16, 32, HttpStatus.INTERNAL_SERVER_ERROR, "PassTicket evaluation failure. Possible reasons include: " +
            "PassTicket to be evaluated is not a successful PassTicket. "
            + "The PassTicket to be evaluated was already evaluated before and replay protection is in effect. "
            + "No PTKTDATA profile exists to match the specified application "
            + "An internal error occurred."),
        ERR_8_16_X(8, 16, null, HttpStatus.INTERNAL_SERVER_ERROR, "PassTicket evaluation extended failure. X'nnnnnnnn' is the internal reason code for the evaluation failure."),
        ERR_UNKNOWN(null, null, null, HttpStatus.INTERNAL_SERVER_ERROR, "The Saf Auth Service returned unknown exception.");


        private final Integer safRc;
        private final Integer racfRc;
        private final Integer racfRsn;

        private final HttpStatus httpStatus;

        private String message;

        public static ErrorCode getErrorCode(AbstractIRRPassTicketException e) {
            for (final ErrorCode ec : values()) {
                if (ec.getSafRc() != null && ec.getSafRc() == e.getSafRc() &&
                    ec.getRacfRc() != null && ec.getRacfRc() == e.getRacfRc() &&
                    ec.getRacfRsn() != null && ec.getRacfRsn() == e.getRacfRsn()) {
                    return ec;
                }
            }

            return ERR_UNKNOWN;
        }
    }
}
