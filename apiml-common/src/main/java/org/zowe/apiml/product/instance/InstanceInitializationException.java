/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.instance;

/**
 * This exception is used to indicate the failed initialization of the instance caused by incorrect metadata.
 */
public class InstanceInitializationException extends RuntimeException {
    private static final long serialVersionUID = -559112794280136165L;

    public InstanceInitializationException(String message) {
        super(message);
    }
}
