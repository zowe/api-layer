/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

export const REQUEST_VERSION_DIFF = 'REQUEST_VERSION_DIFF';
export const RECEIVE_VERSION_DIFF = 'RECEIVE_VERSION_DIFF';

export function getDiff(serviceId, oldVersion, newVersion) {
    // prettier-ignore
    // eslint-disable-next-line no-shadow
    function request(serviceId, oldVersion, newVersion) {   // NOSONAR
        return {
            type: REQUEST_VERSION_DIFF,
            serviceId,
            oldVersion,
            newVersion,
        };
    }

    function receive(diffText) {
        return {
            type: RECEIVE_VERSION_DIFF,
            diffText,
        };
    }

    return dispatch => {
        dispatch(request(serviceId, oldVersion, newVersion));

        return fetch(
            `${process.env.REACT_APP_GATEWAY_URL +
                process.env.REACT_APP_CATALOG_HOME +
                process.env.REACT_APP_APIDOC_UPDATE}/${serviceId}/${oldVersion}/${newVersion}`
        )
            .then(response => response.text())
            .then(text => dispatch(receive(text)))
            .catch(e => {
                // eslint-disable-next-line no-console
                console.log(e);
            });
    };
}
