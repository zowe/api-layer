/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export const inputDataBaseEnabler = [
    {
        text: 'Basic info',
        content: [
            {
                serviceId: {
                    value: 'enablerjavasampleapp',
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
                    value: 'Onboarding Enabler Java Sample App',
                    question: 'The name of the service (human readable):',
                    tooltip: 'Example: Hello API ML',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        nav: 'Basics',
    },
    {
        text: 'Description',
        content: [
            {
                description: {
                    value: 'Example for exposing a Jersey REST API using Onboarding Enabler Java',
                    question: 'A concise description of the service:',
                    tooltip: 'Example: Sample API ML REST Service.',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        nav: 'Basics',
    },
    {
        text: 'Base URL',
        content: [
            {
                baseUrl: {
                    value: 'https://localhost:10016/enablerJavaSampleApp',
                    question: 'The base URL of the service (the consistent part of the web address):',
                    validUrl: true,
                    tooltip: 'https://${samplehost}:${sampleport}/${sampleservice}',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        nav: 'Basics',
    },
    {
        text: 'IP address info',
        content: [
            {
                serviceIpAddress: {
                    value: '127.0.0.1',
                    question: 'The service IP address:',
                    optional: true,
                    regexRestriction: [
                        {
                            value: '^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$',
                            tooltip: 'IP Address v4 expected, example: 127.0.0.1',
                        },
                    ],
                    tooltip: 'Example: 127.0.0.1',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        nav: 'IP info',
    },
    {
        text: 'Prefer IP address',
        content: [
            {
                preferIpAddress: {
                    value: true,
                    question: 'Advertise service IP address instead of its hostname',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        nav: 'IP info',
    },
    {
        text: 'URL',
        content: [
            {
                homePageRelativeUrl: {
                    value: '',
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
        nav: 'URL',
    },
    {
        text: 'Discovery Service URL',
        content: [
            {
                discoveryServiceUrls: {
                    value: 'https://localhost:10011/eureka',
                    question: 'Discovery Service URL:',
                    validUrl: true,
                    tooltip: 'Example: https://localhost:10011/eureka/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
            {
                discoveryServiceUrls: {
                    value: 'https://localhost:10012/eureka',
                    question: 'Discovery Service URL:',
                    validUrl: true,
                    tooltip: 'Example: https://localhost:10011/eureka/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        multiple: true,
        noKey: true,
        indentation: 'discoveryServiceUrls',
        nav: 'Discovery Service URL',
    },
    {
        text: 'Routes',
        content: [
            {
                gatewayUrl: {
                    value: 'api/v1',
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
                    problem: true,
                },
                serviceUrl: {
                    value: '/enablerJavaSampleApp/api/v1',
                    question: 'Service API common context path:',
                    tooltip: 'Example: /sampleservice/api/v1',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
            {
                gatewayUrl: {
                    value: 'ui/v1',
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
                    problem: true,
                },
                serviceUrl: {
                    value: '/',
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
                },
            },
        ],
        help: 'The following service authentication schemes are supported by the API Gateway: bypass, zoweJwt, httpBasicPassTicket, zosmf, x509. ',
        helpUrl: {
            title: 'More information about the authentication parameters',
            link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
        },
        indentation: 'authentication',
        nav: 'Authentication',
    },
    {
        text: 'API Info',
        content: [
            {
                apiId: {
                    value: 'zowe.apiml.enabler.java.sample',
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
                    value: 'api/v1',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Format: /api/vX, Example: /api/v1',
                    regexRestriction: [
                        {
                            value: '^(/[a-z]+\\/v\\d+)$',
                            tooltip: 'Format: /api/vX, Example: /api/v1',
                        },
                    ],
                    disabled: true,
                    interactedWith: true,
                    empty: false,
                    problem: true,
                },
                swaggerUrl: {
                    value: 'https://localhost:10016/enablerJavaSampleApp/openapi.json',
                    question: 'The Http or Https address where the Swagger JSON document is available:',
                    optional: true,
                    tooltip:
                        'Example: http://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                documentationUrl: {
                    value: '',
                    question: 'Link to the external documentation:',
                    optional: true,
                    tooltip: 'Example: https://www.zowe.org',
                },
            },
            {
                apiId: {
                    value: '',
                    question: 'A unique identifier to the API in the API ML:',
                    tooltip: 'Example: zowe.apiml.sampleservice',
                    empty: true,
                },
                version: {
                    value: '',
                    question: 'API version:',
                    tooltip: 'Example: 1.0.0',
                    regexRestriction: [
                        {
                            value: '^(\\d+)\\.(\\d+)\\.(\\d+)$',
                            tooltip: 'Semantic versioning expected, example: 1.0.7',
                        },
                    ],
                    empty: true,
                },
                gatewayUrl: {
                    value: '',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Format: /api/vX, Example: /api/v1',
                    regexRestriction: [
                        {
                            value: '^(/[a-z]+\\/v\\d+)$',
                            tooltip: 'Format: /api/vX, Example: /api/v1',
                        },
                    ],
                    disabled: true,
                },
                swaggerUrl: {
                    value: '',
                    question: 'The Http or Https address where the Swagger JSON document is available:',
                    optional: true,
                    tooltip:
                        'Example: http://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
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
        nav: 'API Info',
        isMinion: true,
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
                    value: 'cademoapps',
                    question: 'The unique identifier for the product family of API services:',
                    tooltip: 'reverse domain name notation. Example: org.zowe.apiml',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                title: {
                    value: 'Sample API Mediation Layer Applications',
                    question: 'The title of the product family of the API service:',
                    tooltip: 'Example: Hello API ML',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                description: {
                    value: 'Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem',
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
        indentation: 'catalog/tile',
        nav: 'Catalog',
    },
    {
        text: 'SSL',
        content: [
            {
                verifySslCertificatesOfServices: {
                    value: true,
                    question: 'Verify SSL certificates of services:',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                protocol: {
                    value: 'TLSv1.2',
                    question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyAlias: {
                    value: 'localhost',
                    question: 'The alias used to address the private key in the keystore',
                    tooltip: 'Example: localhost',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyPassword: {
                    value: 'password',
                    question: 'The password associated with the private key:',
                    type: 'password',
                    tooltip: 'password',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStore: {
                    value: '../keystore/localhost/localhost.keystore.p12',
                    question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                    tooltip: 'Example: keystore/localhost.keystore.p12',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStorePassword: {
                    value: 'password',
                    question: 'The password used to unlock the keystore:',
                    type: 'password',
                    tooltip: 'Your keystore password',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStoreType: {
                    value: 'PKCS12',
                    question: 'Type of the keystore:',
                    options: ['PKCS12', 'JKS', 'JCERACFKS'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStore: {
                    value: '../keystore/localhost/localhost.truststore.p12',
                    question: 'The truststore file used to keep other parties public keys and certificates:',
                    tooltip: 'Example: keystore/localhost.truststore.p12',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStorePassword: {
                    value: 'password',
                    question: 'The password used to unlock the truststore:',
                    type: 'password',
                    tooltip: 'Your truststore password.',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStoreType: {
                    value: 'PKCS12',
                    question: 'Truststore type:',
                    options: ['PKCS12', 'JKS', 'JCERACFKS'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'ssl',
        nav: 'SSL',
    },
];

export const inputDataSpringEnabler = [
    {
        text: 'Spring',
        content: [
            {
                name: {
                    value: '${apiml.service.serviceId}',
                    question: 'This parameter has to be the same as the service ID you are going to provide',
                    hide: true,
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'spring/application',
        nav: 'Basics',
    },
    {
        text: 'Enable',
        content: [
            {
                enabled: {
                    value: true,
                    question: 'Service should automatically register with API ML discovery service',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                enableUrlEncodedCharacters: {
                    value: false,
                    question:
                        'When the value is true, the Gateway allows encoded characters to be part of URL requests redirected through the Gateway. The default setting of false is the recommended setting. Change this setting to true only if you expect certain encoded characters in your applications requests',
                    show: true,
                },
            },
        ],
        indentation: 'apiml',
        nav: 'Basics',
    },
    {
        text: 'Basic info',
        content: [
            {
                serviceId: {
                    value: 'test',
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
                    value: 'Test',
                    question: 'The name of the service (human readable):',
                    tooltip: 'Example: Hello API ML',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Basics',
    },
    {
        text: 'Description',
        content: [
            {
                description: {
                    value: 'A test',
                    question: 'A concise description of the service:',
                    tooltip: 'Example: Sample API ML REST Service.',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Basics',
    },
    {
        text: 'Scheme info',
        content: [
            {
                scheme: {
                    value: 'https',
                    question: 'Service scheme:',
                    tooltip: 'https',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                hostname: {
                    value: 'testhost',
                    question: 'Service hostname:',
                    tooltip:
                        'hostname can be externalized by specifying -Dapiml.service.hostname command line parameter',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                port: {
                    value: '12345',
                    question: 'Service port:',
                    tooltip: 'port can be externalized by specifying -Dapiml.service.port command line parameter',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                contextPath: {
                    value: '/${apiml.service.serviceId}',
                    question: 'Context path:',
                    tooltip:
                        'By default the contextPath is set to be the same as apiml.service.serviceId, but doesnt have to be the same',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Scheme info',
    },
    {
        text: 'Base URL',
        content: [
            {
                baseUrl: {
                    value: 'https://testhost:12345',
                    question: 'The base URL of the service (the consistent part of the web address):',
                    validUrl: true,
                    tooltip: 'https://${samplehost}:${sampleport}/${sampleservice}',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'IP & URL',
    },
    {
        text: 'IP address info',
        content: [
            {
                serviceIpAddress: {
                    value: '',
                    question: 'The service IP address:',
                    optional: true,
                    regexRestriction: [
                        {
                            value: '^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$',
                            tooltip: 'IP Address v4 expected, example: 127.0.0.1',
                        },
                    ],
                    tooltip: 'Example: 127.0.0.1',
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'IP & URL',
    },
    {
        text: 'URL',
        content: [
            {
                homePageRelativeUrl: {
                    value: '${apiml.service.contextPath}/',
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
                    value: '${apiml.service.contextPath}/',
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
                    value: '${apiml.service.contextPath}/',
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
        indentation: 'apiml/service',
        nav: 'IP & URL',
    },
    {
        text: 'Discovery Service URL',
        content: [
            {
                discoveryServiceUrls: {
                    value: 'http://testhost:12345',
                    question: 'Discovery Service URL:',
                    validUrl: true,
                    tooltip: 'Example: https://localhost:10011/eureka/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        multiple: true,
        noKey: true,
        indentation: 'apiml/service/discoveryServiceUrls',
        nav: 'Discovery Service URL',
    },
    {
        text: 'Routes',
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
                serviceUrl: {
                    value: '/enablerJavaSampleApp/api/v1',
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
        indentation: 'apiml/service/routes',
        minions: {
            'API Info': ['gatewayUrl'],
        },
        nav: 'Routes',
    },
    {
        text: 'API Info',
        content: [
            {
                apiId: {
                    value: 'test',
                    question: 'A unique identifier to the API in the API ML:',
                    tooltip: 'Example: zowe.apiml.sampleservice',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                version: {
                    value: '1.0.0',
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
                    value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
                    question: 'The Http or Https address where the Swagger JSON document is available:',
                    optional: true,
                    tooltip:
                        'Example: http://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                documentationUrl: {
                    value: '',
                    question: 'Link to the external documentation:',
                    optional: true,
                    tooltip: 'Example: https://www.zowe.org',
                },
            },
        ],
        indentation: 'apiml/service/apiInfo',
        multiple: true,
        nav: 'API Info',
        isMinion: true,
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
        indentation: 'apiml/service/catalog/tile',
        nav: 'Catalog',
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
                },
            },
        ],
        help: 'The following service authentication schemes are supported by the API Gateway: bypass, zoweJwt, httpBasicPassTicket, zosmf, x509. ',
        helpUrl: {
            title: 'More information about the authentication parameters',
            link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
        },
        indentation: 'apiml/service/authentication',
        nav: 'Auth & SSL',
    },
    {
        text: 'SSL',
        content: [
            {
                verifySslCertificatesOfServices: {
                    value: true,
                    question: 'Verify SSL certificates of services:',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                protocol: {
                    value: 'TL',
                    question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyAlias: {
                    value: 'test',
                    question: 'The alias used to address the private key in the keystore',
                    tooltip: 'Example: localhost',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyPassword: {
                    value: 'test',
                    question: 'The password associated with the private key:',
                    type: 'password',
                    tooltip: 'password',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStore: {
                    value: 'test',
                    question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                    tooltip: 'Example: keystore/localhost.keystore.p12',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStorePassword: {
                    value: 'test',
                    question: 'The password used to unlock the keystore:',
                    type: 'password',
                    tooltip: 'Your keystore password',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStoreType: {
                    value: 'PKCS12',
                    question: 'Type of the keystore:',
                    options: ['PKCS12', 'JKS', 'JCERACFKS'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStore: {
                    value: 'test',
                    question: 'The truststore file used to keep other parties public keys and certificates:',
                    tooltip: 'Example: keystore/localhost.truststore.p12',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStorePassword: {
                    value: 'test',
                    question: 'The password used to unlock the truststore:',
                    type: 'password',
                    tooltip: 'Your truststore password.',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                trustStoreType: {
                    value: 'PKCS12',
                    question: 'Truststore type:',
                    options: ['PKCS12', 'JKS', 'JCERACFKS'],
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service/ssl',
        nav: 'Auth & SSL',
    },
];

export const inputDataMicronautEnabler = [
    {
        text: 'Prefer IP address',
        content: [
            {
                preferIpAddress: {
                    value: true,
                    question: 'Advertise service IP address instead of its hostname',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Basics',
    },
    {
        text: 'Basic info',
        content: [
            {
                serviceId: {
                    value: 'test',
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
                    value: 'Test App',
                    question: 'The name of the service (human readable):',
                    tooltip: 'Example: Hello API ML',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Basics',
    },
    {
        text: 'Description',
        content: [
            {
                description: {
                    value: 'A test app for testing',
                    question: 'A concise description of the service:',
                    tooltip: 'Example: Sample API ML REST Service.',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Basics',
    },
    {
        text: 'Discovery Service URL',
        content: [
            {
                discoveryServiceUrls: {
                    value: 'http://localhost:12345',
                    question: 'Discovery Service URL:',
                    validUrl: true,
                    tooltip: 'Example: https://localhost:10011/eureka/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        multiple: false,
        noKey: true,
        indentation: 'apiml/service',
        nav: 'Scheme Info',
    },
    {
        text: 'Scheme info',
        content: [
            {
                scheme: {
                    value: 'https',
                    question: 'Service scheme:',
                    tooltip: 'https',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                hostname: {
                    value: 'test',
                    question: 'Service hostname:',
                    tooltip:
                        'hostname can be externalized by specifying -Dapiml.service.hostname command line parameter',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                port: {
                    value: '123',
                    question: 'Service port:',
                    tooltip: 'port can be externalized by specifying -Dapiml.service.port command line parameter',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                contextPath: {
                    value: '/${apiml.service.serviceId}',
                    question: 'Context path:',
                    tooltip:
                        "By default the contextPath is set to be the same as apiml.service.serviceId, but doesn't have to be the same",
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'Scheme Info',
    },
    {
        text: 'Base URL',
        content: [
            {
                baseUrl: {
                    value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}',
                    question: 'The base URL of the service (the consistent part of the web address):',
                    validUrl: true,
                    tooltip: 'The URL has to be valid, example: https://localhost:10014',
                    interactedWith: true,
                    empty: false,
                    problem: true,
                },
            },
        ],
        indentation: 'apiml/service',
        nav: 'URL',
    },
    {
        text: 'URL',
        content: [
            {
                homePageRelativeUrl: {
                    value: '${apiml.service.contextPath}',
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
                    value: '${apiml.service.contextPath}',
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
                    value: '${apiml.service.contextPath}',
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
        indentation: 'apiml/service',
        nav: 'URL',
    },
    {
        text: 'Routes',
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
                serviceUrl: {
                    value: '/service/api/v1',
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
        indentation: 'apiml/service/routes',
        minions: {
            'API Info': ['gatewayUrl'],
        },
        nav: 'Routes',
    },
    {
        text: 'API Info for Micronaut',
        content: [
            {
                apiId: {
                    value: 'my.app.for.testing',
                    question: 'A unique identifier to the API in the API ML:',
                    tooltip: 'Example: zowe.apiml.sampleservice',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                version: {
                    value: '1.0.0',
                    question: 'API version:',
                    tooltip: 'Example: 1.0.0',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                gatewayUrl: {
                    value: '${apiml.service.routes.gatewayUrl}',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Format: /api/vX, Example: /api/v1',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'apiml/service/apiInfo',
        multiple: true,
        nav: 'API info',
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
        indentation: 'apiml/service/catalog',
        multiple: true,
        arrIndent: 'tile',
        nav: 'Catalog configuration',
    },
    {
        text: 'SSL detailed',
        content: [
            {
                enabled: {
                    value: false,
                    question: 'Enable SSL:',
                },
                verifySslCertificatesOfServices: {
                    value: '',
                    question: 'Set this parameter to true in production environments:',
                    dependencies: {
                        enabled: true,
                    },
                    empty: true,
                },
                protocol: {
                    value: 'TLSv1.2',
                    question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
                    dependencies: {
                        enabled: true,
                    },
                    hide: true,
                },
                keyAlias: {
                    value: '',
                    question: 'The alias used to address the private key in the keystore:',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip: 'Your key alias',
                    empty: true,
                },
                keyPassword: {
                    value: '',
                    question: 'The password associated with the private key:',
                    dependencies: {
                        enabled: true,
                    },
                    type: 'password',
                    tooltip: 'Your key password',
                    empty: true,
                },
                keyStore: {
                    value: '',
                    question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip: 'Your keystore',
                    empty: true,
                },
                keyStorePassword: {
                    value: '',
                    question: 'The password used to unlock the keystore:',
                    dependencies: {
                        enabled: true,
                    },
                    type: 'password',
                    tooltip: 'Your keystore password',
                    empty: true,
                },
                keyStoreType: {
                    value: '',
                    question: 'Type of the keystore:',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip: 'Your keystore type: example: PKCS12',
                    empty: true,
                },
                trustStore: {
                    value: '',
                    question: 'The truststore file used to keep other parties public keys and certificates:',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip: 'Example: keystore/localhost.truststore.p12',
                    empty: true,
                },
                trustStorePassword: {
                    value: '',
                    question: 'The password used to unlock the truststore:',
                    dependencies: {
                        enabled: true,
                    },
                    type: 'password',
                    tooltip: 'Your truststore password.',
                    empty: true,
                },
                trustStoreType: {
                    value: 'PKCS12',
                    question: 'Truststore type:',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip: 'Your truststore type. Example: PKCS12',
                },
                ciphers: {
                    value: '',
                    question: 'SSL cipher suites:',
                    dependencies: {
                        enabled: true,
                    },
                    tooltip:
                        'Ciphers that are used by the HTTPS servers in API ML services and can be externalized by specifying -Dapiml.security.ciphers command line parameter.',
                    empty: true,
                },
            },
        ],
        indentation: 'apiml/service/ssl',
        multiple: true,
        nav: 'SSL',
    },
    {
        text: 'Micronaut',
        content: [
            {
                name: {
                    value: '${apiml.service.serviceId}',
                    question: 'Application name:',
                    tooltip: 'Example Hello API ML',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/application',
        nav: 'Micronaut configuration',
    },
    {
        text: 'Micronaut ports',
        content: [
            {
                port: {
                    value: '${apiml.service.port}',
                    question: 'The port to be used:',
                    tooltip: 'Port on which the service listens',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'context-path': {
                    value: '/${apiml.service.serviceId}',
                    question: 'Application context path:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/server',
        nav: 'Micronaut configuration',
    },
    {
        text: 'Micronaut SSL enable',
        content: [
            {
                enable: {
                    value: false,
                    question: 'Enabled SSL:',
                },
            },
        ],
        indentation: 'micronaut/ssl',
        nav: 'Micronaut SSL configuration',
    },
    {
        text: 'Micronaut SSL key-store',
        content: [
            {
                password: {
                    value: '${apiml.service.ssl[0].keyPassword}',
                    question: 'The password associated with the private key:',
                    tooltip: 'password',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                type: {
                    value: '${apiml.service.ssl[0].keyStoreType}',
                    question: 'Type of the keystore:',
                    tooltip: 'Your keystore type: example: PKCS12',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                path: {
                    value: 'file:${apiml.service.ssl[0].keyStore}',
                    question: 'The keystore file used to store the private key:',
                    tooltip: 'Example: ssl/localhost.keystore.key',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/ssl/key-store',
        nav: 'Micronaut SSL configuration',
    },
    {
        text: 'Micronaut SSL key',
        content: [
            {
                alias: {
                    value: '${apiml.service.ssl[0].keyAlias}',
                    question: 'The alias used to address the private key in the keystore:',
                    tooltip: 'Your key alias',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                password: {
                    value: '${apiml.service.ssl[0].keyPassword}',
                    question: 'The password associated with the private key:',
                    tooltip: 'password',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/ssl/key',
        nav: 'Micronaut SSL configuration',
    },
    {
        text: 'Micronaut SSL trust-store',
        content: [
            {
                password: {
                    value: '${apiml.service.ssl[0].trustStorePassword}',
                    question: 'The password used to unlock the truststore:',
                    type: 'password',
                    tooltip: 'Your truststore password.',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                path: {
                    value: 'file:${apiml.service.ssl[0].trustStore}',
                    question: 'The truststore file used to keep other parties public keys and certificates:',
                    tooltip: 'Example: keystore/localhost.truststore.p12',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                type: {
                    value: '${apiml.service.ssl[0].trustStoreType}',
                    question: 'Truststore type:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/ssl/trust-store',
        nav: 'Micronaut configuration',
    },
    {
        text: 'Micronaut config',
        content: [
            {
                port: {
                    value: '${apiml.service.port}',
                    question: 'Service port:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                ciphers: {
                    value: '${apiml.service.ssl[0].ciphers}',
                    question: 'SSL cipher suites:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                protocol: {
                    value: '${apiml.service.ssl[0].protocol}',
                    question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'micronaut/ssl',
        nav: 'Micronaut configuration',
    },
    {
        text: 'Micronaut Eureka',
        content: [
            {
                defaultZone: {
                    value: 'https://localhost:44212',
                    question: 'Eureka default zone:',
                    tooltip: 'Example: https://localhost:10011/eureka/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'eureka/client/serviceUrl',
        nav: 'Micronaut management',
    },
    {
        text: 'Micronaut management base-path',
        content: [
            {
                'base-path': {
                    value: '/thing',
                    question: 'Endpoint base-path:',
                    tooltip: 'Example: /application',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'management/endpoints/web',
        nav: 'Micronaut management',
    },
    {
        text: 'Micronaut management exposure',
        content: [
            {
                include: {
                    value: 'yep,yepp',
                    question: 'Endpoint base-path:',
                    tooltip: 'Example: health,info',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'management/endpoints/web/exposure',
        nav: 'Micronaut management',
    },
    {
        text: 'Micronaut management health',
        content: [
            {
                enabled: {
                    value: false,
                    question: 'Enable management health:',
                },
            },
        ],
        indentation: 'management/endpoints/health/defaults',
        nav: 'Micronaut management',
    },
];

export const inputDataNodeJSEnabler = [
    {
        text: 'Eureka',
        content: [
            {
                ssl: {
                    value: false,
                    question: 'Turn SSL on for Eureka',
                    show: true,
                },
                host: {
                    value: 'localhost',
                    question: 'The host to be used:',
                    tooltip: 'Example: localhost',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                ipAddress: {
                    value: '127.0.0.1',
                    question: 'The IP address to be used:',
                    tooltip: 'Example: 127.0.0.1',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                port: {
                    value: '10011',
                    question: 'The port to be used:',
                    tooltip: 'Example: 10011',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                servicePath: {
                    value: '/eureka/apps/',
                    question: 'The service path:',
                    tooltip: 'Example: /eureka/apps/',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                maxRetries: {
                    value: 30,
                    question: 'The maximum number of retries:',
                    hide: true,
                    tooltip: 'Number of retries before failing. Example: 30',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                requestRetryDelay: {
                    value: 1000,
                    question: 'The request retry delay:',
                    hide: true,
                    tooltip: 'Milliseconds to wait between retries. Example: 1000',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                registryFetchInterval: {
                    value: 5,
                    question: 'The interval for registry interval:',
                    hide: true,
                    tooltip:
                        'How often does Eureka client pull the service list from Eureka server. The default is 30 seconds. Example: 5',
                    show: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'eureka',
        nav: 'Basics',
    },
    {
        text: 'Instance',
        content: [
            {
                app: {
                    value: '${serviceId}',
                    question: 'App ID:',
                    tooltip: 'Example: hwexpress',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                vipAddress: {
                    value: '${serviceId}',
                    question: 'Virtual IP address:',
                    tooltip: 'Example: hwexpress',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                instanceId: {
                    value: 'localhost:hwexpress:10020',
                    question: 'Instance ID:',
                    tooltip: 'Example: localhost:hwexpress:10020',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                homePageUrl: {
                    value: '${homePageRelativeUrl}',
                    question: 'The URL of the home page:',
                    tooltip: 'Example: https://localhost:10020/',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                hostname: {
                    value: 'localhost',
                    question: 'Host name:',
                    tooltip: 'Example: localhost',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                ipAddr: {
                    value: '127.0.0.1',
                    question: 'IP address:',
                    tooltip: 'Example: 127.0.0.1',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                secureVipAddress: {
                    value: '${serviceId}',
                    question: 'Secure virtual IP address:',
                    tooltip: 'Example: hwexpress',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'instance',
        nav: 'Instance',
    },
    {
        text: 'Instance port',
        content: [
            {
                $: {
                    value: '10020',
                    question: 'Port:',
                    tooltip: 'Example: 10020',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                '@enabled': {
                    value: false,
                    question: 'Enable?',
                },
            },
        ],
        indentation: 'instance/port',
        nav: 'Instance ports',
    },
    {
        text: 'Instance security port',
        content: [
            {
                $: {
                    value: '10020',
                    question: 'Security port:',
                    tooltip: 'Example: 10020',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                '@enabled': {
                    value: true,
                    question: 'Enable?',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'instance/securePort',
        nav: 'Instance ports',
    },
    {
        text: 'Data center info',
        content: [
            {
                '@class': {
                    value: 'com.test',
                    question: 'Class:',
                    tooltip: 'Example: com.netflix.appinfo. InstanceInfo$DefaultDataCenterInfo',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                name: {
                    value: 'Test',
                    question: 'Name:',
                    tooltip: 'Example: MyOwn',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'instance/dataCenterInfo',
        nav: 'Instance info',
    },
    {
        text: 'Metadata',
        content: [
            {
                'apiml.catalog.tile.id': {
                    value: 'samplenodeservice',
                    question: 'Tile ID for the API ML catalog:',
                    tooltip: 'Example: samplenodeservice',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.catalog.tile.title': {
                    value: 'Zowe TEST',
                    question: 'Tile title for the API ML catalog:',
                    tooltip: 'Example: Zowe Sample Node Service',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.catalog.tile.description': {
                    value: 'test',
                    question: 'Tile description for the API ML catalog:',
                    tooltip: 'Example: NodeJS Sample service running',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.catalog.tile.version': {
                    value: '1.1.1',
                    question: 'Tile version for the API ML catalog:',
                    tooltip: 'Example: 1.0.0',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.routes.api_v1.gatewayUrl': {
                    value: '${routes.gatewayUrl}',
                    question: 'API gateway URL:',
                    tooltip: 'Example: api/v1',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.routes.api_v1.serviceUrl': {
                    value: '${routes.serviceUrl}',
                    question: 'API service URL:',
                    tooltip: 'Example: /api/v1',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.apiInfo.0.apiId': {
                    value: 'test.test',
                    question: 'A unique identifier to the API in the API ML:',
                    tooltip: 'Example: zowe.apiml.hwexpress',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.apiInfo.0.gatewayUrl': {
                    value: '${routes.gatewayUrl}',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Example: api/v1',
                    hide: true,
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.apiInfo.0.swaggerUrl': {
                    value: 'http://localhost:10020/swagger.json',
                    question: 'The base path at the API Gateway where the API is available:',
                    tooltip: 'Example: https://localhost:10020/ swagger.json',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.service.title': {
                    value: 'Test Service',
                    question: 'Service title:',
                    tooltip: 'Example: Zowe Sample Node Service',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                'apiml.service.description': {
                    value: 'a service for testing',
                    question: 'Service description:',
                    tooltip:
                        'Example: The Proxy Server is an HTTP HTTPS, and Websocket server built upon NodeJS and ExpressJS.',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'instance/metadata',
        nav: 'Instance metadata',
    },
    {
        text: 'SSL for Node',
        content: [
            {
                certificate: {
                    value: 'ssl/localhost.keystore.cer',
                    question: 'Certificate:',
                    tooltip: 'Example: ssl/localhost.keystore.cer',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyStore: {
                    value: 'ssl/localhost.keystore.cer',
                    question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                    tooltip: 'Example: ssl/localhost.keystore.key',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                caFile: {
                    value: 'ssl/localhost.kpen',
                    question: 'Certificate Authority file:',
                    tooltip: 'Example: ssl/localhost.pem',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
                keyPassword: {
                    value: '123',
                    question: 'The password associated with the private key:',
                    type: 'password',
                    tooltip: 'password',
                    interactedWith: true,
                    empty: false,
                    problem: false,
                },
            },
        ],
        indentation: 'ssl',
        nav: 'SSL',
    },
];

export const inputDataStaticOnboarding = [
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
                    value: 'http://localhost:8080',
                    question: 'The base URL of the instance (the consistent part of the web address):',
                    tooltip: 'Example: http://localhost:8080',
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
                        'Example: http://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
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
];
