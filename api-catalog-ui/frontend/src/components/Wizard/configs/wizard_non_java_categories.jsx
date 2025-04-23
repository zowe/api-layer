/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
const createField = (question, tooltip = '', options = {}) => ({
    value: options.value !== undefined ? options.value : '',
    question,
    tooltip,
    ...(options.type ? { type: options.type } : {}),
    ...(options.hide !== undefined ? { hide: options.hide } : {}),
});

export const nonJavaSpecificCategories = [
    {
        text: 'SSL for ',
        content: {
            certificate: createField('Certificate:', 'Example: ssl/localhost.keystore.cer'),
            keyStore: createField(
                'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                'Example: ssl/localhost.keystore.key'
            ),
            caFile: createField('Certificate Authority file:', 'Example: ssl/localhost.pem'),
            keyPassword: createField(
                'The password associated with the private key:',
                'password',
                { type: 'password' }
            ),
        },
    },
    {
        text: 'Eureka',
        content: {
            ssl: { value: false, question: 'Turn SSL on for Eureka' },
            host: createField('The host to be used:', 'Example: localhost'),
            ipAddress: createField('The IP address to be used:', 'Example: 127.0.0.1'),
            port: createField('The port to be used:', 'Example: 10011'),
            servicePath: createField('The service path:', 'Example: /eureka/apps/'),
            maxRetries: createField('The maximum number of retries:', 'Number of retries before failing. Example: 30', { hide: true }),
            requestRetryDelay: createField('The request retry delay:', 'Milliseconds to wait between retries. Example: 1000', { hide: true }),
            registryFetchInterval: createField(
                'The interval for registry interval:',
                'How often does Eureka client pull the service list from Eureka server. The default is 30 seconds. Example: 5',
                { hide: true }
            ),
        },
    },
    {
        text: 'Instance',
        content: {
            app: createField('App ID:', 'Example: sampleservice'),
            vipAddress: createField('Virtual IP address:', 'Example: sampleservice'),
            instanceId: createField('Instance ID:', 'Example: localhost:sampleservice:10018'),
            homePageUrl: createField('The URL of the home page:', 'Example: https://localhost:10018/'),
            statusPageUrl: createField('The status page:', 'Example: /application/info'),
            healthCheckUrl: createField('The health check page:', 'Example: /application/health'),
            hostname: createField('Host name:', 'Example: localhost'),
            ipAddr: createField('IP address:', 'Example: 127.0.0.2'),
            secureVipAddress: createField('Secure virtual IP address:', 'Example: sampleservice'),
            port: createField('Port:', 'Example: 10018'),
            nonSecurePortEnabled: { value: false, question: 'Enable?' },
            securePort: createField('Security port:', 'Example: 10018'),
            securePortEnabled: { value: true, question: 'Enable?' },
        },
    },
    {
        text: 'Metadata',
        content: {
            'apiml.catalog.tile.id': createField('Tile ID for the API ML catalog:', 'Example: sampleservice'),
            'apiml.catalog.tile.title': createField('Tile title for the API ML catalog:', 'Example: Zowe Sample Service'),
            'apiml.catalog.tile.description': createField('Tile description for the API ML catalog:', 'Example: Sample service running'),
            'apiml.catalog.tile.version': createField('Tile version for the API ML catalog:', 'Example: 1.0.0'),
            'apiml.routes.api_v1.gatewayUrl': createField('API gateway URL:', 'Example: api/v1'),
            'apiml.routes.api_v1.serviceUrl': createField('API service URL:', 'Example: /sampleservice'),
            'apiml.apiInfo.0.apiId': createField('A unique identifier to the API in the API ML:', 'Example: zowe.apiml.sampleservice'),
            'apiml.apiInfo.0.gatewayUrl': createField('The base path at the API Gateway where the API is available:', 'Example: api/v1'),
            'apiml.apiInfo.0.swaggerUrl': createField('The base path at the API Gateway where the API is available:', 'Example: https://localhost:10018/apidoc'),
            'apiml.service.title': createField('Service title:', 'Example: Zowe Sample  Service'),
            'apiml.service.description': createField('Service description:', 'Sample API services'),
            'apiml.apiInfo.0.version': createField('The version:', 'Example: 1.0.1'),
        },
    },
];
