import * as log from 'loglevel';
import uuidv4 from 'uuid/v4';
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
