/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { toast } from 'react-toastify';
import {
    FETCH_TILES_FAILED,
    FETCH_TILES_RETRY,
    FETCH_TILES_REQUEST,
    FETCH_TILES_STOP,
    FETCH_TILES_SUCCESS,
} from '../constants/catalog-tile-constants';

const fetchRetryToastId = 9998;

export function fetchTilesFailed(error) {
    // send a notification toast message (do not duplicate or auto close)
    toast.dismiss(fetchRetryToastId);
    return {
        type: FETCH_TILES_FAILED,
        payload: error,
    };
}

export function fetchTilesSuccess(tiles) {
    // dismiss the notification if it is displayed
    toast.dismiss(fetchRetryToastId);
    return {
        type: FETCH_TILES_SUCCESS,
        payload: tiles,
    };
}

export function fetchTilesStop() {
    // dismiss the notification if it is displayed
    toast.dismiss(fetchRetryToastId);
    return {
        type: FETCH_TILES_STOP,
    };
}

export function fetchTilesRetry(retryAttempt, maxRetries) {
    toast.warn(
        `Could not get a response when fetching tile info, retrying (attempt ${retryAttempt} of ${maxRetries})`,
        {
            autoClose: false,
            toastId: fetchRetryToastId,
        }
    );
    return {
        type: FETCH_TILES_RETRY,
    };
}

export function fetchTilesStart(id) {
    // dismiss the notification if it is displayed
    toast.dismiss(fetchRetryToastId);
    return {
        type: FETCH_TILES_REQUEST,
        payload: id,
    };
}
