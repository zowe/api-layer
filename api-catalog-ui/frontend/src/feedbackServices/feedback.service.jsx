/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import * as log from 'loglevel';

function handleResponse(response) {
    return response.text().then((text) => {
        const data = text ? JSON.parse(text) : {};
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

function getToken() {
    const allowOrigin = checkOrigin();
    const requestOptions = {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'X-Requested-With': 'XMLHttpRequest',
        },
    };

    return fetch(`https://s3805888.t.eloqua.com/e/formsubmittoken?elqSiteID=3805888`, {
        method: 'GET',
    })
        .then((data) => data)
        .catch((error) => {
            log.error('Logout process failed', error);
            throw new Error(error);
        });
}

function submitFeedback(feedbackdata) {
    // eslint-disable-next-line no-console
    console.log(feedbackdata);
    const allowOrigin = checkOrigin();
    const requestOptions = {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest',
        },
        body: feedbackdata,
    };
    return fetch(`https://s3805888.t.eloqua.com/e/f2`, requestOptions)
        .then(handleResponse)
        .then((user) => user);
}

// eslint-disable-next-line
export const feedbackService = {
    getToken,
    submitFeedback,
};
