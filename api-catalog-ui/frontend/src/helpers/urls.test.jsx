/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import getBaseUrl from "./urls";

describe('>>> Urls Tests', () => {
    it('should return the Gateway URL if used', () => {
        const environment = {
            REACT_APP_GATEWAY_URL: '',
            REACT_APP_CATALOG_HOME: ''
        }

        const location = {
            protocol: 'https:',
            host: 'localhost:10010',
            pathname: '/apicatalog/ui/v1',
        };

        expect(getBaseUrl(environment, location)).toEqual('https://localhost:10010/apicatalog/api/v1');
    });

    it('should return the information stored in env if the URL isn\'t the gateway one', () => {
        const environment = {
            REACT_APP_GATEWAY_URL: 'https://localhost:10010',
            REACT_APP_CATALOG_HOME: '/apicatalog/api/v1'
        }

        const location = {
            protocol: 'https:',
            host: 'localhost:3000',
            pathname: '/',
        };

        expect(getBaseUrl(environment, location)).toEqual('https://localhost:10010/apicatalog/api/v1');
    });

    it('should return the current URL if none above, access via standalone Catalog', () => {
        const environment = {
            REACT_APP_GATEWAY_URL: '',
            REACT_APP_CATALOG_HOME: ''
        }

        const location = {
            protocol: 'https:',
            host: 'localhost:10014',
            pathname: '/apicatalog',
        };

        expect(getBaseUrl(environment, location)).toEqual('https://localhost:10014/apicatalog');
    });
});