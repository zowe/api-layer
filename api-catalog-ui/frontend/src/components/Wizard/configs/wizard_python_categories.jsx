/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export const pythonSpecificCategories = [
    {
        text: 'PythonBasics',
        content: {
            host: {
                value: '',
                question: 'The host to be used:',
                tooltip: 'Example: localhost',
            },
            ipAddress: {
                value: '',
                question: 'The IP address to be used:',
                tooltip: 'Example: 127.0.0.1',
            },
            homePageUrl: {
                value: '',
                question: 'The homepage:',
                tooltip: 'Example: https://localhost:10019/',
            },
            statusPageUrl: {
                value: '',
                question: 'The status page:',
                tooltip: 'Example: /application/info',
            },
            port: {
                value: '',
                question: 'The port to be used:',
                tooltip: 'Example: 10011',
            },
        },
    },
    {
        text: 'PythonMetadata',
        content: {
            'apiml.catalog.tile.id': {
                value: '',
                question: 'Tile ID for the API ML catalog:',
                tooltip: 'Example: samplepythonservice',
            },
            'apiml.catalog.tile.title': {
                value: '',
                question: 'Tile title for the API ML catalog:',
                tooltip: 'Example: Zowe Sample python Service',
            },
            'apiml.catalog.tile.description': {
                value: '',
                question: 'Tile description for the API ML catalog:',
                tooltip: 'Example: python Sample service running',
            },
            'apiml.catalog.tile.version': {
                value: '',
                question: 'Tile version for the API ML catalog:',
                tooltip: 'Example: 1.0.0',
            },
            'apiml.routes.api_v1.gatewayUrl': {
                value: '',
                question: 'API gateway URL:',
                tooltip: 'Example: api/v1',
            },
            'apiml.routes.api_v1.serviceUrl': {
                value: '',
                question: 'API service URL:',
                tooltip: 'Example: /api/v1',
            },
            'apiml.service.id': {
                value: '',
                question: 'Service Id:',
                tooltip: 'Example: python_enabler',
            },
            'apiml.service.title': {
                value: '',
                question: 'Service title:',
                tooltip: 'Example: Zowe Sample python Service',
            },
            'apiml.service.description': {
                value: '',
                question: 'Service description:',
                tooltip:
                    'Example: The Proxy Server is an HTTP HTTPS, and Websocket server built upon python and ExpressJS.',
            },
            'apiml.apiInfo.0.swaggerUrl': {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
                tooltip: 'Example: https://localhost:10020/ swagger.json',
            },
            'apiml.apiInfo.0.apiId': {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
                tooltip: 'Example: zowe.apiml.hwexpress',
            },
            'apiml.apiInfo.0.version': {
                value: '',
                question: 'The version:',
                tooltip: 'Example: 1.0.1',
            },
        },
    },
];
