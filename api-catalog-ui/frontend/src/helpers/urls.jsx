/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * Return base URL. It should work for environment where the API Catalog is behind the
 * gateway as well as in a standalone environments. It should also correctly work behind
 * the Gateway for different service ids for the API Catalog.
 *
 * @param pEnvironment {Optional} Provide environment to be used to get the Gateway URL
 * @param pLocation {Optional} Provide custom location object
 * @returns Valid Base Url without ending /
 */

const getBaseUrl = (pEnvironment, pLocation) => {
    const location = pLocation || window.location;
    const environment = pEnvironment || process.env;

    const urlParts = location.pathname.split('/');
    if (urlParts[2] === 'ui') {
        return `${location.protocol}//${location.host}/${urlParts[1]}/api/${urlParts[3]}`;
    }

    if (environment.REACT_APP_GATEWAY_URL && environment.REACT_APP_CATALOG_HOME) {
        return `${environment.REACT_APP_GATEWAY_URL}${environment.REACT_APP_CATALOG_HOME}`;
    }

    return `${location.protocol}//${location.host}/apicatalog`;
};

export default getBaseUrl;
