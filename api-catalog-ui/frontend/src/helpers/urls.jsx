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
 * @returns Valid Base Url without ending /
 */

const getBaseUrl = () => {
    const urlParts = window.location.pathname.split('/');
    if (urlParts[2] === 'ui') {
        return `${window.location.protocol}//${window.location.host}/${urlParts[1]}/api/${urlParts[3]}`;
    }

    if (process.env.REACT_APP_GATEWAY_URL && process.env.REACT_APP_CATALOG_HOME) {
        return `${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}`;
    }

    return `${window.location.protocol}//${window.location.host}/apicatalog`;
};

export default getBaseUrl;
