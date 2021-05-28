/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import log from 'loglevel';
import { v4 as uuidv4 } from 'uuid';

import { SEND_ERROR, CLEAR_ALL_ERRORS } from '../constants/error-constants';

export function sendError(error) {
    const uuid = uuidv4();
    const err = { id: uuid, timestamp: new Date(), error };
    log.error(`Error: ${err}`);
    return {
        type: SEND_ERROR,
        payload: err,
    };
}

export function clearAllErrors() {
    return {
        type: CLEAR_ALL_ERRORS,
        payload: null,
    };
}
