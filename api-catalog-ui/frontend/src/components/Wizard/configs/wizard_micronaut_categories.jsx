// eslint-disable-next-line import/prefer-default-export
export const micronautSpecificCategories = [
    {
        text: 'API Info for Micronaut',
        content: {
            apiId: {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
            },
            version: {
                value: '',
                question: 'API version:',
            },
            gatewayUrl: {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
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
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
                dependencies: { enabled: true },
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                dependencies: { enabled: true },
            },
            keyStorePassword: {
                value: '',
                question: 'The password used to unlock the keystore:',
                dependencies: { enabled: true },
            },
            keyStoreType: {
                value: '',
                question: 'Type of the keystore:',
                dependencies: { enabled: true },
            },
            trustStore: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
                dependencies: { enabled: true },
            },
            trustStorePassword: {
                value: '',
                question: 'The password used to unlock the truststore:',
                dependencies: { enabled: true },
            },
            trustStoreType: {
                value: 'PKCS12',
                question: 'Truststore type:',
                dependencies: { enabled: true },
            },
            ciphers: {
                value: '',
                question: 'SSL cipher suites:',
                dependencies: { enabled: true },
            },
        },
    },
    {
        text: 'Micronaut',
        content: {
            name: {
                value: '',
                question: 'Application name:',
            },
        },
    },
    {
        text: 'Micronaut ports',
        content: {
            port: {
                value: '',
                question: 'The port to be used:',
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
            },
            type: {
                value: '',
                question: 'The type of key store:',
            },
            path: {
                value: '',
                question: 'The keystore file used to store the private key:',
            },
        },
    },
    {
        text: 'Micronaut SSL key',
        content: {
            alias: {
                value: '',
                question: 'The alias used to address the private key in the keystore:',
            },
            password: {
                value: '',
                question: 'The password associated with the private key:',
            },
        },
    },
    {
        text: 'Micronaut SSL trust-store',
        content: {
            password: {
                value: '',
                question: 'The password used to unlock the truststore:',
            },
            path: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
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
            },
        },
    },
    {
        text: 'Micronaut management base-path',
        content: {
            'base-path': {
                value: '',
                question: 'Endpoint base-path:',
            },
        },
    },
    {
        text: 'Micronaut management exposure',
        content: {
            include: {
                value: '',
                question: 'Endpoint base-path:',
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
