/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.instance;

/**
 * Exception used to indicate that the instance aren't initialized properly because of incorrect metadata.
 */
public class InstanceInitializationException extends RuntimeException {
    private static final long serialVersionUID = -559112794280136165L;

    public InstanceInitializationException(String message) {
        super(message);
    }
}
