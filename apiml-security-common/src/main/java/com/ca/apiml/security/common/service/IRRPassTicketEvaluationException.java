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

/**
 * Exception on evaluation of passTicket
 */
public class IRRPassTicketEvaluationException extends AbstractIRRPassTicketException {

    private static final long serialVersionUID = -7401871844111323433L;

    public IRRPassTicketEvaluationException(int safRc, int racfRc, int racfRsn) {
        super(safRc, racfRc, racfRsn);
    }

    @Override
    public String getMessage() {
        return getMessage("Error on evaluation of PassTicket:");
    }
}
