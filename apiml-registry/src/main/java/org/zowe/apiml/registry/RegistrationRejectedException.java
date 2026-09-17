/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

/**
 * Thrown to refuse a registration.
 * <p>
 * Used sparingly. Most conformance problems - a non-RFC-952 service id, an inconsistent appName, a PassTicket
 * scheme with no applid - are reported as warnings and the registration proceeds, because services with those
 * defects register successfully today and failing them on upgrade would take working systems down. Only a
 * genuine policy refusal, such as a host outside the configured allow-list, rejects.
 */
public class RegistrationRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegistrationRejectedException(String message) {
        super(message);
    }

    public RegistrationRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

}
