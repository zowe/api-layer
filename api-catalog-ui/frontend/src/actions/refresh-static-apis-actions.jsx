import { toast } from 'react-toastify';
import {
    REFRESH_STATIC_APIS_SUCCESS,
    REFRESH_STATIC_APIS_ERROR,
    CLEAR_ERROR,
} from '../constants/refresh-static-apis-constants';

export function refreshStaticApisSuccess() {
    toast.success('The refresh of static APIs was successful!', {
        closeOnClick: true,
        autoClose: 2000,
    });
    return {
        type: REFRESH_STATIC_APIS_SUCCESS,
        refreshTimestamp: Date.now(),
    };
}

export function refreshStaticApisError(error) {
    return {
        type: REFRESH_STATIC_APIS_ERROR,
        error,
    };
}

export function refreshedStaticApi() {
    const url = `${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}/static-api/refresh`;
    return dispatch => {
        fetch(url, {
            method: 'POST',
        })
            .then(res => res.json())
            // eslint-disable-next-line no-use-before-define
            .then(fetchHandler, error => dispatch(refreshStaticApisError(error)))
            .then(() => dispatch(refreshStaticApisSuccess()))
            .catch(error => {
                dispatch(refreshStaticApisError(error));
            });
    };
}

export function clearError() {
    return {
        type: CLEAR_ERROR,
        error: null,
    };
}

function fetchHandler(res) {
    const errors = [];
    if (res && !res.errors) {
        return Promise.reject(res.messages[0]);
    } else if (res && res.errors && res.errors.length !== 0) {
        res.errors.forEach(item => {
            errors.push(item.convertedText);
        });
        return Promise.reject(errors);
    }
    return res;
}
