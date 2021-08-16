/**
 * each new category:
 * 1. must contain properties:
 *  1.1. text - the name of the category
 *  1.2. content - object containing sub-objects (each with a value and a question key)
 * 2. can contain properties:
 *  2.1. multiple - boolean, if true, allows for multiple sets of configuration
 *  2.2. indentation - string, nests object like so: 'a/b' - { a:{ b:your_object } }
 */
// eslint-disable-next-line import/prefer-default-export
export const categoryData = [
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
        },
    },
    {
        text: 'Prefer IP address',
        content: {
            preferIpAddress: {
                value: false,
                question: 'Advertise service IP address instead of its hostname',
                optional: true,
            },
        },
    },
    {
        text: 'Scheme info',
        content: {
            scheme: {
                value: '',
                question: 'Service scheme (https by default):',
            },
            hostname: {
                value: '',
                question: 'Service hostname:',
            },
            port: {
                value: '',
                question: 'Service port:',
            },
            contextPath: {
                value: '',
                question: 'Context path:',
            },
        },
    },
    {
        text: 'IP address info',
        content: {
            baseUrl: {
                value: '',
                question: 'The base URL of the service (the consistent part of the web address):',
            },
            serviceIpAddress: {
                value: '',
                question: 'The service IP address:',
                optional: true,
            },
        },
    },
    {
        text: 'URL',
        content: {
            homePageRelativeUrl: {
                value: '',
                question: 'The relative path to the home page of the service:',
                optional: true,
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
    },
    {
        text: 'URL for Static',
        content: {
            instanceBaseUrls: {
                value: '',
                question: 'The base URL of the instance (the consistent part of the web address):',
            },
        },
        multiple: false,
        noKey: true,
    },
    {
        text: 'Discovery Service URL',
        content: {
            discoveryServiceHost: {
                value: '',
                question: 'Discovery Service URL:',
            },
        },
        multiple: true,
        noKey: true,
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
        multiple: true,
    },
    {
        text: 'Routes for Static',
        content: {
            gatewayUrl: {
                value: '',
                question: 'The portion of the gateway URL which is replaced by the serviceUrl path part:',
            },
            serviceRelativeUrl: {
                value: '',
                question: 'A portion of the service instance URL path which replaces the gatewayUrl part:',
            },
        },
        multiple: true,
    },
    {
        text: 'Authentication',
        content: {
            scheme: {
                value: '',
                question: 'Authentication:',
                options: ['bypass', 'zoweJwt', 'httpBasicPassTicket', 'zosmf', 'x509', 'headers'],
            },
            applid: {
                value: '',
                question: 'A service APPLID (valid only for the httpBasicPassTicket authentication scheme ):',
            },
        },
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
                question: 'The Http or Https address where the Swagger JSON document is available:',
                optional: true,
            },
            documentationUrl: {
                value: '',
                question: 'Link to the external documentation:',
                optional: true,
            },
        },
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
    },
    {
        text: 'Enable',
        content: {
            enabled: {
                value: false,
                question: 'Service should automatically register with API ML discovery service',
            },
            enableUrlEncodedCharacters: {
                value: false,
                question: 'Service requests the API ML GW to receive encoded characters in the URL',
            },
        },
    },
    {
        text: 'Spring',
        content: {
            name: {
                value: '',
                question: 'This parameter has to be the same as the service ID you are going to provide',
            },
        },
    },
];
