import { REFRESH_STATIC_APIS_ERROR } from '../constants/refresh-static-apis-constants';

export function refreshStaticApisError(error) {
    return {
        type: REFRESH_STATIC_APIS_ERROR,
        error: error
    }
}

function checkOrigin() {
    // only allow the gateway url to authenticate the user
    let allowOrigin = process.env.REACT_APP_GATEWAY_URL;
    if (
        process.env.REACT_APP_GATEWAY_URL === null ||
        process.env.REACT_APP_GATEWAY_URL === undefined ||
        process.env.REACT_APP_GATEWAY_URL === ''
    ) {
        allowOrigin = window.location.origin;
    }
    if (allowOrigin === null || allowOrigin === undefined) {
        throw new Error('Allow Origin is not set for Login/Logout process');
    }
    return allowOrigin;
}

export function refreshedStaticApi() {
    console.log("hi")
    const refreshEndpoint = '/discovery/api/v1/staticApi';
    const url =
        'https://localhost:10011' + refreshEndpoint;
    return dispatch => {
        fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': 'Basic dXNlcjp1c2Vy',
                'Content-Type': 'application/json',
                // 'Access-Control-Allow-Methods': 'POST',
                // 'Access-Control-Allow-Origin': process.env.REACT_APP_DISCOVERY_URL
            }
        })
            .then(error => {
                dispatch(refreshStaticApisError(error));
            });
    }
}

