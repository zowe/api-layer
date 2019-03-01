/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.client.exception;

/**
 * An exception is thrown when the pet ID is mismatched
 */
public class PetIdMismatchException extends RuntimeException {
    private final Long pathId;
    private final Long bodyId;

    public PetIdMismatchException(String message, Long pathId, Long bodyId) {
        super(message);
        this.pathId = pathId;
        this.bodyId = bodyId;
    }

    public Long getPathId() {
        return pathId;
    }

    public Long getBodyId() {
        return bodyId;
    }
}
