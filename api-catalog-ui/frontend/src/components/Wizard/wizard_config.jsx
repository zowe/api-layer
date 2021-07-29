// eslint-disable-next-line import/prefer-default-export
export const data = [
    {
        text: 'Basic info',
        content: {
            serviceId: {
                value: '',
                question: 'A unique identifier for the API (max 40 characters, lowercase):',
            },
            title: {
                value: '',
                question: 'The name of the service (human readable):',
            },
            description: {
                value: '',
                question: 'A concise description of the service:',
            },
            baseUrl: {
                value: '',
                question: 'The base URL of the service (the consistent part of the web address):',
            },
            serviceIpAddress: {
                value: '',
                question: 'The service IP address (optional):',
            },
            preferIpAddress: {
                value: '',
                question: 'Set to true to advertise a service IP address instead of its hostname (optional):',
            },
        },
        multiple: false,
    },
    {
        text: 'URL',
        content: {
            homePageRelativeUrl: {
                value: '',
                question: 'The relative path to the home page of the service (if it has one):',
            },
            statusPageRelativeUrl: {
                value: '',
                question: 'The relative path to the status page of the service:',
            },
            healthCheckRelativeUrl: {
                value: '',
                question: 'The relative path to the health check endpoint of the service:',
            },
        },
        multiple: false,
    },
    {
        text: 'Discovery Service URL',
        content: {
            discoveryService1: {
                value: '',
                question: 'An URL for the Discovery Service',
            },
            discoveryService2: {
                value: '',
                question: 'An URL for the Discovery Service',
            },
        },
        multiple: true,
    },
    {
        text: 'Routes',
        content: {
            gatewayUrl: {
                value: '',
                question: 'The portion of the gateway URL which is replaced by the serviceUrl path part:',
            },
            serviceUrl: {
                value: '',
                question: 'A portion of the service instance URL path which replaces the gatewayUrl part:',
            },
        },
        multiple: false,
    },
    {
        text: 'Authentication',
        content: {
            scheme: {
                value: '',
                question: 'Authentication (bypass, zoweJwt, httpBasicPassTicket, zosmf, x509, headers):',
            },
            applid: {
                value: '',
                question: 'A service APPLID (valid only for the httpBasicPassTicket authentication scheme ):',
            },
        },
        multiple: false,
    },
    {
        text: 'API Info',
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
            swaggerUrl: {
                value: '',
                question: 'The Http or Https address where the Swagger JSON document is available (optional):',
            },
            documentationUrl: {
                value: '',
                question: 'Link to the external documentation (optional):',
            },
        },
        multiple: false,
    },
    {
        text: 'Catalog',
        content: {
            id: {
                value: '',
                question: 'The unique identifier for the product family of API services:',
            },
            title: {
                value: '',
                question: 'The title of the product family of the API service:',
            },
            description: {
                value: '',
                question: 'A description of the API service product family:',
            },
            version: {
                value: '',
                question: 'The semantic version of this API Catalog tile (increase when adding changes):',
            },
        },
        multiple: false,
    },
    {
        text: 'SSL',
        content: {
            verifySslCertificatesOfServices: {
                value: '',
                question: 'Set this parameter to true in production environments:',
            },
            protocol: {
                value: '',
                question:
                    'The TLS protocol version used by Zowe API ML Discovery Service (recommendation: use TLSv1.2):',
            },
            keyAlias: {
                value: '',
                question: 'The alias used to address the private key in the keystore',
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
            },
            keyStorePassword: {
                value: '',
                question: 'The password used to unlock the keystore:',
            },
            keyStoreType: {
                value: '',
                question: 'Type of the keystore:',
            },
            trustStore: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
            },
            trustStorePassword: {
                value: '',
                question: 'The password used to unlock the truststore:',
            },
            trustStoreType: {
                value: '',
                question: 'Truststore type (the default value is PKCS12):',
            },
        },
        multiple: false,
    },
];

export const enablerData = [
    {
        text: 'Plain Java Enabler',
        categories: [
            { name: 'Basic info', indentation: false },
            { name: 'URL', indentation: false },
            { name: 'Routes', indentation: 'routes', multiple: true },
            { name: 'Authentication', indentation: 'authentication', multiple: false },
            { name: 'API Info', indentation: 'apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'catalog/tiles' },
            { name: 'SSL', indentation: 'ssl' },
        ],
    },
    {
        text: 'Spring Enabler',
    },
    {
        text: 'Micronaut Enabler',
    },
    {
        text: 'Node JS Enabler',
    },
    {
        text: 'Static Onboarding',
    },
    {
        text: 'Direct Call to Eureka',
    },
];
