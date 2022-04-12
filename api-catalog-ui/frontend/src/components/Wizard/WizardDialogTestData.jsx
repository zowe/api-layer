export const inputDataCombinedEnabler = [
    {
        text: 'Basic info',
        content: [
            {
                serviceId: {
                    value: 'sampleservice',
                    question: 'A unique identifier for the API (service ID):',
                    maxLength: 40,
                    lowercase: true,
                    tooltip: 'Example: sampleservice',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                title: {
                    value: 'Hello API ML',
                    question: 'The name of the service (human readable):',
                    tooltip: 'Example: Hello API ML',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        inArr: true,
        nav: 'Basics',
    },
    {
        text: 'Description',
        content: [
            {
                description: {
                    value: 'Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem',
                    question: 'A concise description of the service:',
                    tooltip: 'Example: Sample API ML REST Service.',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        inArr: true,
        nav: 'Basics',
    },
    {
        text: 'Catalog info',
        content: [
            {
                type: {
                    value: 'Custom',
                    question: 'Choose existing catalog tile or create a new one:',
                    options: ['Custom'],
                    hidden: true,
                    show: true,
                },
                catalogUiTileId: {
                    value: 'apicatalog',
                    question: 'The id of the catalog tile:',
                    regexRestriction: [
                        {
                            value: '^[a-zA-Z1-9]+$',
                            tooltip: 'Only alphanumerical values with no whitespaces are accepted',
                        },
                    ],
                    dependencies: {
                        type: 'Custom',
                    },
                    tooltip: 'Example: static',
                    show: true,
                },
            },
        ],
        interference: 'staticCatalog',
        inArr: true,
        nav: 'Basics',
    },
    {
        text: 'URL for Static',
        content: [
            {
                instanceBaseUrls: {
                    value: 'https://localhost:8080',
                    question: 'The base URL of the instance (the consistent part of the web address):',
                    tooltip: 'Example: https://localhost:8080',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        multiple: true,
        noKey: true,
        indentation: 'instanceBaseUrls',
        inArr: true,
        nav: 'URL',
    },
    {
        text: 'URL',
        content: [
            {
                homePageRelativeUrl: {
                    value: '/home',
                    question: 'The relative path to the home page of the service:',
                    optional: true,
                    regexRestriction: [
                        {
                            value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
                            tooltip: 'The relative URL has to be valid, example: /application/info',
                        },
                    ],
                    tooltip:
                        'Normally used for informational purposes for other services to use it as a landing page. Example: /home',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                statusPageRelativeUrl: {
                    value: '/application/info',
                    question: 'The relative path to the status page of the service:',
                    optional: true,
                    regexRestriction: [
                        {
                            value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
                            tooltip: 'The relative URL has to be valid, example: /application/info',
                        },
                    ],
                    tooltip: 'Example: /application/info',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                healthCheckRelativeUrl: {
                    value: '/application/health',
                    question: 'The relative path to the health check endpoint of the service:',
                    optional: true,
                    regexRestriction: [
                        {
                            value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
                            tooltip: 'The relative URL has to be valid, example: /application/info',
                        },
                    ],
                    tooltip: 'Example: /application/health',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        inArr: true,
        nav: 'URL',
    },
    {
        text: 'Routes for Static & Node',
        content: [
            {
                gatewayUrl: {
                    value: '/api/v1',
                    question: 'Expose the Service API on Gateway under context path:',
                    tooltip: 'Format: /api/vX, Example: /api/v1',
                    regexRestriction: [
                        {
                            value: '^(/[a-z]+\\/v\\d+)$',
                            tooltip: 'Format: /api/vX, Example: /api/v1',
                        },
                    ],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                serviceRelativeUrl: {
                    value: '/sampleservice/api/v1',
                    question: 'Service API common context path:',
                    tooltip: 'Example: /sampleservice/api/v1',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        help: 'For service: <service>/allOfMyEndpointsAreHere/** exposed on Gateway under <gateway>/<serviceid>/api/v1/**\nFill in:\ngatewayUrl: /api/v1\nserviceUrl: /allOfMyEndpointsAreHere',
        multiple: true,
        indentation: 'routes',
        inArr: true,
        minions: {
            'API Info': ['gatewayUrl'],
        },
        nav: 'Routes',
    },
    {
        text: 'Authentication',
        content: [
            {
                scheme: {
                    value: 'bypass',
                    question: 'Authentication:',
                    options: ['bypass', 'zoweJwt', 'httpBasicPassTicket', 'zosmf', 'x509'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                applid: {
                    value: '',
                    question: 'A service APPLID (valid only for the httpBasicPassTicket authentication scheme ):',
                    dependencies: {
                        scheme: 'httpBasicPassTicket',
                    },
                    tooltip: 'Example: ZOWEAPPL',
                    empty: true,
                },
                headers: {
                    value: 'X-Certificate-Public',
                    question:
                        'For the x509 scheme use the headers parameter to select which values to send to a service',
                    dependencies: {
                        scheme: 'x509',
                    },
                    options: ['X-Certificate-Public', 'X-Certificate-DistinguishedName', 'X-Certificate-CommonName'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        help: 'The following service authentication schemes are supported by the API Gateway: bypass, zoweJwt, httpBasicPassTicket, zosmf, x509. ',
        helpUrl: {
            title: 'More information about the authentication parameters',
            link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
        },
        indentation: 'authentication',
        inArr: true,
        nav: 'Authentication',
    },
    {
        text: 'API Info',
        content: [
            {
                apiId: {
                    value: 'test.test.test',
                    question: 'A unique identifier to the API in the API ML:',
                    tooltip: 'Example: zowe.apiml.sampleservice',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                version: {
                    value: '1.1.1',
                    question: 'API version:',
                    tooltip: 'Example: 1.0.0',
                    regexRestriction: [
                        {
                            value: '^(\\d+)\\.(\\d+)\\.(\\d+)$',
                            tooltip: 'Semantic versioning expected, example: 1.0.7',
                        },
                    ],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                gatewayUrl: {
                    value: '/api/v1',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Format: api/vX, Example: api/v1',
                    regexRestriction: [
                        {
                            value: '^(/[a-z]+\\/v\\d+)$',
                            tooltip: 'Format: /api/vX, Example: /api/v1',
                        },
                    ],
                    disabled: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                swaggerUrl: {
                    value: '',
                    question: 'The Http or Https address where the Swagger JSON document is available:',
                    optional: true,
                    tooltip:
                        'Example: https://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
                },
                documentationUrl: {
                    value: '',
                    question: 'Link to the external documentation:',
                    optional: true,
                    tooltip: 'Example: https://www.zowe.org',
                },
            },
        ],
        indentation: 'apiInfo',
        multiple: true,
        inArr: true,
        nav: 'API Info',
        isMinion: true,
    },
    {
        text: 'Catalog UI Tiles',
        content: [
            {
                title: {
                    value: 'API Mediation Layer API',
                    question: 'The title of the API services product family:',
                    tooltip: 'Example: Static API services',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                description: {
                    value: 'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                    question: 'The detailed description of the API Catalog UI dashboard tile:',
                    tooltip:
                        'Example: Services which demonstrate how to make an API service discoverable in the API ML ecosystem using YAML definitions',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentationDependency: 'catalogUiTileId',
        indentation: 'catalogUiTiles',
        nav: 'Catalog UI Tiles',
    },
    {
        text: 'Catalog',
        content: [
            {
                type: {
                    value: 'Custom',
                    question: 'Choose existing catalog tile or create a new one:',
                    options: ['Custom'],
                    hidden: true,
                },
                id: {
                    value: 'apicatalog',
                    question: 'The unique identifier for the product family of API services:',
                    tooltip: 'reverse domain name notation. Example: org.zowe.apiml',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                title: {
                    value: 'API Mediation Layer API',
                    question: 'The title of the product family of the API service:',
                    tooltip: 'Example: Hello API ML',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                description: {
                    value: 'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                    question: 'A description of the API service product family:',
                    tooltip: 'Example: Sample application to demonstrate exposing a REST API in the ZOWE API ML',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                version: {
                    value: '1.0.0',
                    question: 'The semantic version of this API Catalog tile (increase when adding changes):',
                    tooltip: 'Example: 1.0.0',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        interference: 'catalog',
        indentation: 'catalog',
        multiple: true,
        arrIndent: 'tile',
        nav: 'Catalog configuration',
    },
];