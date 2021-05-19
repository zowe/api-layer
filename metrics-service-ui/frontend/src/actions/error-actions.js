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
