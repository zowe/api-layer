/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const MAX_LENGTH_MAINFRAME_ID = 8;
const MAX_LENGTH_DISTRIBUTED_ID = 246;
const MAX_LENGTH_LABEL = 32;

export function hasValidLength(parameter: string, maxSize: number): boolean {
    return parameter.length <= maxSize;
}

export function isValidMainframeId(mainframeId: string) {
    return hasValidLength(mainframeId, MAX_LENGTH_MAINFRAME_ID) && /^[a-zA-Z0-9#$@]+$/.test(mainframeId);
}

export function isValidDistributedId(distributedId: string) {
    return hasValidLength(distributedId, MAX_LENGTH_DISTRIBUTED_ID) && /^[a-zA-Z0-9_#$@. -]*$/.test(distributedId);
}

export function isValidUserName(userName: string) {
    return hasValidLength(userName, MAX_LENGTH_LABEL) && /^[a-zA-Z0-9#$@. ]*$/.test(userName);
}
