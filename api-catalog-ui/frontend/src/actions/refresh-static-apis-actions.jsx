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
    REFRESH_STATIC_APIS_SUCCESS,
    REFRESH_STATIC_APIS_ERROR,
    CLEAR_ERROR,
} from '../constants/refresh-static-apis-constants';
import getBaseUrl from '../helpers/urls';

export function refreshStaticApisSuccess() {
    toast.success('The refresh of static APIs was successful!', {
        closeOnClick: true,
        autoClose: 2000,
    });
    setTimeout(() => {
        toast.dismiss();
    }, 2000);
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
    const url = `${getBaseUrl()}/static-api/refresh`;
    return (dispatch) => {
        fetch(url, { method: 'POST' })
            .then((res) => {
                if (!res.ok) {
                    return res.text().then(text => {
                        let errorJson;
                        try {
                            errorJson = JSON.parse(text);
                        } catch {
                            errorJson = {
                                messageNumber: 'ZWEAD702E',
                                messageContent: text || res.statusText,
                                messageType: 'ERROR'
                            };
                        }
                        dispatch(refreshStaticApisError(errorJson));
                        throw errorJson;
                    });
                }
                return res.json();
            })
            .then((data) => {
                fetchHandler(data)
                    .catch((errors) => {
                        console.log(`refresh static apis returned warnings: ${JSON.stringify(errors)}`)
                    })
                    .then(_ => dispatch(refreshStaticApisSuccess()));
            })
            .catch((error) => {
                console.error("Error refreshing static APIs:", error);
                if (error.messageNumber) {
                    dispatch(refreshStaticApisError(error));
                } else {
                    dispatch(refreshStaticApisError({
                        messageNumber: 'ZWEAD703E',
                        messageContent: error.message || 'Network error',
                        messageType: 'ERROR'
                    }));
                }
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
        errors.push(res.messages[0])
        return Promise.reject(errors);
    }
    if (res && res.errors && res.errors.length !== 0) {
        res.errors.forEach((item) => {
            errors.push(item.convertedText);
        });
        return Promise.reject(errors);
    }
    return Promise.resolve();
}
