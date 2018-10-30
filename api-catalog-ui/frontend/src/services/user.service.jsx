import * as log from 'loglevel';

function handleResponse(response) {
    return response.json().then(data => {
        if (!response.ok) {
            const [message] = data.messages;
            return Promise.reject(message);
        }

        return data;
    });
}

function checkOrigin() {
    // only allow the gateway url to authenticate the user
    let allowOrigin = process.env.REACT_APP_GATEWAY_URL;
    if (process.env.REACT_APP_GATEWAY_URL === null || process.env.REACT_APP_GATEWAY_URL === undefined) {
        allowOrigin = window.location.origin;
    }
    if (allowOrigin === null || allowOrigin === undefined) {
        throw new Error('Allow Origin is not set for Logout process');
    }
    return allowOrigin;
}

function logout() {
    const allowOrigin = checkOrigin();
    const requestOptions = {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Access-Control-Allow-Origin': allowOrigin,
            'Access-Control-Allow-Credentials': 'true',
        },
    };

    return fetch(`${process.env.REACT_APP_CATALOG_HOME}/auth/logout`, requestOptions)
        .then(data => data)
        .catch(error => {
            log.error('Logout process failed', error);
            throw new Error(error);
        });
}

function login(credentials) {
    const allowOrigin = checkOrigin();
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': allowOrigin,
            'Access-Control-Allow-Credentials': 'true',
        },
        body: JSON.stringify(credentials),
    };
    return fetch(`${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}/auth/login`, requestOptions)
        .then(handleResponse)
        .then(user => user);
}

// eslint-disable-next-line
export const userService = {
    login,
    logout,
};
