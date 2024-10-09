/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export const micronautSpecificCategories = [
    {
        text: 'API Info for Micronaut',
        content: {
            apiId: {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
                tooltip: 'Example: zowe.apiml.sampleservice',
            },
            version: {
                value: '',
                question: 'API version:',
                tooltip: 'Example: 1.0.0',
            },
            gatewayUrl: {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
                tooltip: 'Format: /api/vX, Example: /api/v1',
            },
        },
    },
    {
        text: 'SSL detailed',
        content: {
            enabled: {
                value: false,
                question: 'Enable SSL:',
            },
            verifySslCertificatesOfServices: {
                value: '',
                question: 'Set this parameter to true in production environments:',
                dependencies: { enabled: true },
            },
            protocol: {
                value: 'TLSv1.2',
                question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
                dependencies: { enabled: true },
            },
            keyAlias: {
                value: '',
                question: 'The alias used to address the private key in the keystore:',
                dependencies: { enabled: true },
                tooltip: 'Your key alias',
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
                dependencies: { enabled: true },
                type: 'password',
                tooltip: 'Your key password',
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                dependencies: { enabled: true },
                tooltip: 'Your keystore',
            },
            keyStorePassword: {
                value: '',
                question: 'The password used to unlock the keystore:',
                dependencies: { enabled: true },
                type: 'password',
                tooltip: 'Your keystore password',
            },
            keyStoreType: {
                value: '',
                question: 'Type of the keystore:',
                dependencies: { enabled: true },
                tooltip: 'Your keystore type: example: PKCS12',
            },
            trustStore: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
                dependencies: { enabled: true },
                tooltip: 'Example: keystore/localhost.truststore.p12',
            },
            trustStorePassword: {
                value: '',
                question: 'The password used to unlock the truststore:',
                dependencies: { enabled: true },
                type: 'password',
                tooltip: 'Your truststore password.',
            },
            trustStoreType: {
                value: 'PKCS12',
                question: 'Truststore type:',
                dependencies: { enabled: true },
                tooltip: 'Your truststore type. Example: PKCS12',
            },
            ciphers: {
                value: '',
                question: 'SSL cipher suites:',
                dependencies: { enabled: true },
                tooltip:
                    'Ciphers that are used by the HTTPS servers in API ML services and can be externalized by specifying -Dapiml.security.ciphers command line parameter.',
            },
        },
    },
    {
        text: 'Micronaut',
        content: {
            name: {
                value: '',
                question: 'Application name:',
                tooltip: 'Example Hello API ML',
            },
        },
    },
    {
        text: 'Micronaut ports',
        content: {
            port: {
                value: '',
                question: 'The port to be used:',
                tooltip: 'Port on which the service listens',
            },
            'context-path': {
                value: '',
                question: 'Application context path:',
            },
        },
    },
    {
        text: 'Micronaut SSL enable',
        content: {
            enable: {
                value: false,
                question: 'Enabled SSL:',
            },
        },
    },
    {
        text: 'Micronaut SSL key-store',
        content: {
            password: {
                value: '',
                question: 'The password associated with the private key:',
                tooltip: 'password',
            },
            type: {
                value: '',
                question: 'Type of the keystore:',
                tooltip: 'Your keystore type: example: PKCS12',
            },
            path: {
                value: '',
                question: 'The keystore file used to store the private key:',
                tooltip: 'Example: ssl/localhost.keystore.key',
            },
        },
    },
    {
        text: 'Micronaut SSL key',
        content: {
            alias: {
                value: '',
                question: 'The alias used to address the private key in the keystore:',
                tooltip: 'Your key alias',
            },
            password: {
                value: '',
                question: 'The password associated with the private key:',
                tooltip: 'password',
            },
        },
    },
    {
        text: 'Micronaut SSL trust-store',
        content: {
            password: {
                value: '',
                question: 'The password used to unlock the truststore:',
                type: 'password',
                tooltip: 'Your truststore password.',
            },
            path: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
                tooltip: 'Example: keystore/localhost.truststore.p12',
            },
            type: {
                value: '',
                question: 'Truststore type:',
            },
        },
    },
    {
        text: 'Micronaut config',
        content: {
            port: {
                value: '',
                question: 'Service port:',
            },
            ciphers: {
                value: '',
                question: 'SSL cipher suites:',
            },
            protocol: {
                value: '',
                question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
            },
        },
    },
    {
        text: 'Micronaut Eureka',
        content: {
            defaultZone: {
                value: '',
                question: 'Eureka default zone:',
                tooltip: 'Example: https://localhost:10011/eureka/',
            },
        },
    },
    {
        text: 'Micronaut management base-path',
        content: {
            'base-path': {
                value: '',
                question: 'Endpoint base-path:',
                tooltip: 'Example: /application',
            },
        },
    },
    {
        text: 'Micronaut management exposure',
        content: {
            include: {
                value: '',
                question: 'Endpoint base-path:',
                tooltip: 'Example: health,info',
            },
        },
    },
    {
        text: 'Micronaut management health',
        content: {
            enabled: {
                value: false,
                question: 'Enable management health:',
            },
        },
    },
];
