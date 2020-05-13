import { REFRESH_STATIC_APIS_SUCCESS, REFRESH_STATIC_APIS_ERROR } from '../constants/refresh-static-apis-constants';

export function refreshStaticApisSuccess() {
    return {
        type: REFRESH_STATIC_APIS_SUCCESS,
        refreshTimestamp: Date.now()
    }
}

export function refreshStaticApisError(error) {
    return {
        type: REFRESH_STATIC_APIS_ERROR,
        error
    }
}

export function refreshedStaticApi() {
    const url =
        `${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}/discovery/api/v1/staticApi`;
    return dispatch => {
        fetch(url, {
            method: 'POST'
        })
            .then(fetchHandler, error => dispatch(refreshStaticApisError(error)))
            .then(() => dispatch(refreshStaticApisSuccess()))
            .catch(error => {
                dispatch(refreshStaticApisError(error));
            });
    }

}

function fetchHandler(res) {
    if (res.status >= 400 && res.status < 600) {
        return Promise.reject(res);
    }
    return res.json();
}

