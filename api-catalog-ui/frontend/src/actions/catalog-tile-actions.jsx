import { toast } from 'react-toastify';
import {
    FETCH_TILES_FAILED,
    FETCH_TILES_RETRY,
    FETCH_TILES_REQUEST,
    FETCH_TILES_STOP,
    FETCH_TILES_SUCCESS,
    FETCH_SERVICE_DOC,
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
    let payload;
    if (id){
        payload = `${process.env.REACT_APP_CATALOG_UPDATE}/${id}`;
    } else {
        payload = process.env.REACT_APP_CATALOG_UPDATE
    }
    return {
        type: FETCH_TILES_REQUEST,
        payload,
    };
}