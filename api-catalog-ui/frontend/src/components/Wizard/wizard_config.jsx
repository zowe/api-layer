import { defaultSpring } from './wizard_defaults';

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
        },
        multiple: false,
    },
    {
        text: 'Prefer IP address',
        content: {
            preferIpAddress: {
                value: '',
                question: 'Set to true to advertise a service IP address instead of its hostname (optional):',
            },
        },
        multiple: false,
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
        multiple: false,
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
                question: 'The service IP address (optional):',
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
            discoveryServiceHost: {
                value: '',
                question: 'Discovery Service host:',
            },
            discoveryServicePort: {
                value: '',
                question: 'Discovery Service port:',
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
    {
        text: 'Enable',
        content: {
            enabled: {
                value: '',
                question:
                    'Decision if the service should automatically register with API ML discovery service (true/false):',
            },
            enableUrlEncodedCharacters: {
                value: '',
                question:
                    'Decision if the service requests the API ML GW to receive encoded characters in the URL (true/false):',
            },
        },
        multiple: false,
    },
    {
        text: 'Spring',
        content: {
            name: {
                value: '',
                question: 'This parameter has to be the same as the service ID you are going to provide',
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
            { name: 'IP address info', indentation: false },
            { name: 'Prefer IP address', indentation: false },
            { name: 'URL', indentation: false },
            { name: 'Discovery Service URL', indentation: 'discoveryServiceUrls', multiple: true },
            { name: 'Routes', indentation: 'routes', multiple: true },
            { name: 'Authentication', indentation: 'authentication', multiple: false },
            { name: 'API Info', indentation: 'apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'catalog/tiles' },
            { name: 'SSL', indentation: 'ssl' },
        ],
    },
    {
        text: 'Spring Enabler',
        categories: [
            { name: 'Spring', indentation: 'spring/application' },
            { name: 'Enable', indentation: 'apiml' },
            { name: 'Basic info', indentation: 'apiml/service' },
            { name: 'Scheme info', indentation: 'apiml/service' },
            { name: 'IP address info', indentation: 'apiml/service' },
            { name: 'URL', indentation: 'apiml/service' },
            { name: 'Discovery Service URL', indentation: 'apiml/service/discoveryServiceUrls', multiple: true },
            { name: 'Routes', indentation: 'apiml/service/routes', multiple: true },
            { name: 'Authentication', indentation: 'apiml/service/authentication', multiple: false },
            { name: 'API Info', indentation: 'apiml/service/apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'apiml/service/catalog/tiles' },
            { name: 'SSL', indentation: 'apiml/service/ssl' },
        ],
        defaults: defaultSpring,
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
