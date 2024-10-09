/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export const nodeSpecificCategories = [
    {
        text: 'SSL for Node',
        content: {
            certificate: {
                value: '',
                question: 'Certificate:',
                tooltip: 'Example: ssl/localhost.keystore.cer',
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                tooltip: 'Example: ssl/localhost.keystore.key',
            },
            caFile: {
                value: '',
                question: 'Certificate Authority file:',
                tooltip: 'Example: ssl/localhost.pem',
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
                type: 'password',
                tooltip: 'password',
            },
        },
    },
    {
        text: 'Eureka',
        content: {
            ssl: {
                value: false,
                question: 'Turn SSL on for Eureka',
            },
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
            port: {
                value: '',
                question: 'The port to be used:',
                tooltip: 'Example: 10011',
            },
            servicePath: {
                value: '',
                question: 'The service path:',
                tooltip: 'Example: /eureka/apps/',
            },
            maxRetries: {
                value: '',
                question: 'The maximum number of retries:',
                hide: true,
                tooltip: 'Number of retries before failing. Example: 30',
            },
            requestRetryDelay: {
                value: '',
                question: 'The request retry delay:',
                hide: true,
                tooltip: 'Milliseconds to wait between retries. Example: 1000',
            },
            registryFetchInterval: {
                value: '',
                question: 'The interval for registry interval:',
                hide: true,
                tooltip:
                    'How often does Eureka client pull the service list from Eureka server. The default is 30 seconds. Example: 5',
            },
        },
    },
    {
        text: 'Instance',
        content: {
            app: {
                value: '',
                question: 'App ID:',
                tooltip: 'Example: hwexpress',
            },
            vipAddress: {
                value: '',
                question: 'Virtual IP address:',
                tooltip: 'Example: hwexpress',
            },
            instanceId: {
                value: '',
                question: 'Instance ID:',
                tooltip: 'Example: localhost:hwexpress:10020',
            },
            homePageUrl: {
                value: '',
                question: 'The URL of the home page:',
                tooltip: 'Example: https://localhost:10020/',
            },
            hostname: {
                value: '',
                question: 'Host name:',
                tooltip: 'Example: localhost',
            },
            ipAddr: {
                value: '',
                question: 'IP address:',
                tooltip: 'Example: 127.0.0.1',
            },
            secureVipAddress: {
                value: '',
                question: 'Secure virtual IP address:',
                tooltip: 'Example: hwexpress',
            },
        },
    },
    {
        text: 'Instance port',
        content: {
            $: {
                value: '',
                question: 'Port:',
                tooltip: 'Example: 10020',
            },
            '@enabled': {
                value: false,
                question: 'Enable?',
            },
        },
    },
    {
        text: 'Instance security port',
        content: {
            $: {
                value: '',
                question: 'Security port:',
                tooltip: 'Example: 10020',
            },
            '@enabled': {
                value: true,
                question: 'Enable?',
            },
        },
    },
    {
        text: 'Data center info',
        content: {
            '@class': {
                value: '',
                question: 'Class:',
                tooltip: 'Example: com.netflix.appinfo. InstanceInfo$DefaultDataCenterInfo',
            },
            name: {
                value: '',
                question: 'Name:',
                tooltip: 'Example: MyOwn',
            },
        },
    },
    {
        text: 'Metadata',
        content: {
            'apiml.catalog.tile.id': {
                value: '',
                question: 'Tile ID for the API ML catalog:',
                tooltip: 'Example: samplenodeservice',
            },
            'apiml.catalog.tile.title': {
                value: '',
                question: 'Tile title for the API ML catalog:',
                tooltip: 'Example: Zowe Sample Node Service',
            },
            'apiml.catalog.tile.description': {
                value: '',
                question: 'Tile description for the API ML catalog:',
                tooltip: 'Example: NodeJS Sample service running',
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
            'apiml.apiInfo.0.apiId': {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
                tooltip: 'Example: zowe.apiml.hwexpress',
            },
            'apiml.apiInfo.0.gatewayUrl': {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
                tooltip: 'Example: api/v1',
            },
            'apiml.apiInfo.0.swaggerUrl': {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
                tooltip: 'Example: https://localhost:10020/ swagger.json',
            },
            'apiml.service.title': {
                value: '',
                question: 'Service title:',
                tooltip: 'Example: Zowe Sample Node Service',
            },
            'apiml.service.description': {
                value: '',
                question: 'Service description:',
                tooltip:
                    'Example: The Proxy Server is an HTTP HTTPS, and Websocket server built upon NodeJS and ExpressJS.',
            },
        },
    },
];
