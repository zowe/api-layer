/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { wizRegex } from './wizard_regex_restrictions';

export const baseCategories = [
    {
        text: 'Basic info',
        content: {
            serviceId: {
                value: '',
                question: 'A unique identifier for the API (service ID):',
                maxLength: 40,
                lowercase: true,
                tooltip: 'Example: sampleservice',
            },
            title: {
                value: '',
                question: 'The name of the service (human readable):',
                tooltip: 'Example: Hello API ML',
            },
        },
    },
    {
        text: 'Description',
        content: {
            description: {
                value: '',
                question: 'A concise description of the service:',
                tooltip: 'Example: Sample API ML REST Service.',
            },
        },
    },
    {
        text: 'Base URL',
        content: {
            baseUrl: {
                value: '',
                question: 'The base URL of the service (the consistent part of the web address):',
                validUrl: true,
                tooltip: 'https://${samplehost}:${sampleport}/${sampleservice}',
            },
        },
    },
    {
        text: 'Prefer IP address',
        content: {
            preferIpAddress: {
                value: false,
                question: 'Advertise service IP address instead of its hostname',
            },
        },
    },
    {
        text: 'Scheme info',
        content: {
            scheme: {
                value: 'https',
                question: 'Service scheme:',
                tooltip: 'https',
            },
            hostname: {
                value: '',
                question: 'Service hostname:',
                tooltip: 'hostname can be externalized by specifying -Dapiml.service.hostname command line parameter',
            },
            port: {
                value: '',
                question: 'Service port:',
                tooltip: 'port can be externalized by specifying -Dapiml.service.port command line parameter',
            },
            contextPath: {
                value: '',
                question: 'Context path:',
                tooltip:
                    "By default the contextPath is set to be the same as apiml.service.serviceId, but doesn't have to be the same",
            },
        },
    },
    {
        text: 'IP address info',
        content: {
            serviceIpAddress: {
                value: '',
                question: 'The service IP address:',
                optional: true,
                regexRestriction: [wizRegex.ipAddress],
                tooltip: 'Example: 127.0.0.1',
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
                regexRestriction: [wizRegex.validRelativeUrl],
                tooltip:
                    'Normally used for informational purposes for other services to use it as a landing page. Example: /home',
            },
            statusPageRelativeUrl: {
                value: '',
                question: 'The relative path to the status page of the service:',
                optional: true,
                regexRestriction: [wizRegex.validRelativeUrl],
                tooltip: 'Example: /application/info',
            },
            healthCheckRelativeUrl: {
                value: '',
                question: 'The relative path to the health check endpoint of the service:',
                optional: true,
                regexRestriction: [wizRegex.validRelativeUrl],
                tooltip: 'Example: /application/health',
            },
        },
    },
    {
        text: 'Discovery Service URL',
        content: {
            discoveryServiceUrls: {
                value: '',
                question: 'Discovery Service URL:',
                validUrl: true,
                tooltip: 'Example: https://localhost:10011/eureka/',
            },
        },
        multiple: false,
        noKey: true,
    },
    {
        text: 'Routes',
        content: {
            gatewayUrl: {
                value: '',
                question: 'Expose the Service API on Gateway under context path:',
                tooltip: 'Format: /api/vX, Example: /api/v1',
                regexRestriction: [wizRegex.gatewayUrl],
            },
            serviceUrl: {
                value: '',
                question: 'Service API common context path:',
                tooltip: 'Example: /sampleservice/api/v1',
            },
        },
        help: 'For service: <service>/allOfMyEndpointsAreHere/** exposed on Gateway under <gateway>/<serviceid>/api/v1/**\nFill in:\ngatewayUrl: /api/v1\nserviceUrl: /allOfMyEndpointsAreHere',
        multiple: true,
    },

    {
        text: 'Authentication',
        content: {
            scheme: {
                value: 'bypass',
                question: 'Authentication:',
                options: ['bypass', 'zoweJwt', 'httpBasicPassTicket', 'zosmf', 'x509'],
            },
            applid: {
                value: '',
                question: 'A service APPLID (valid only for the httpBasicPassTicket authentication scheme ):',
                dependencies: { scheme: 'httpBasicPassTicket' },
                tooltip: 'Example: ZOWEAPPL',
            },
            headers: {
                value: 'X-Certificate-Public',
                question: 'For the x509 scheme use the headers parameter to select which values to send to a service',
                dependencies: { scheme: 'x509' },
                options: ['X-Certificate-Public', 'X-Certificate-DistinguishedName', 'X-Certificate-CommonName'],
            },
        },
        help: 'The following service authentication schemes are supported by the API Gateway: bypass, zoweJwt, httpBasicPassTicket, zosmf, x509. ',
        helpUrl: {
            title: 'More information about the authentication parameters',
            link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
        },
    },
    {
        text: 'API Info',
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
                regexRestriction: [wizRegex.version],
            },
            gatewayUrl: {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
                tooltip: 'Format: api/vX, Example: api/v1',
                regexRestriction: [wizRegex.gatewayUrl],
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
    },
    {
        text: 'Catalog',
        content: {
            type: {
                value: 'Custom',
                question: 'Choose existing catalog tile or create a new one:',
                options: ['Custom'],
                hidden: true,
            },
            id: {
                value: '',
                question: 'The unique identifier for the product family of API services:',
                tooltip: 'reverse domain name notation. Example: org.zowe.apiml',
            },
            title: {
                value: '',
                question: 'The title of the product family of the API service:',
                tooltip: 'Example: Hello API ML',
            },
            description: {
                value: '',
                question: 'A description of the API service product family:',
                tooltip: 'Example: Sample application to demonstrate exposing a REST API in the ZOWE API ML',
            },
            version: {
                value: '',
                question: 'The semantic version of this API Catalog tile (increase when adding changes):',
                tooltip: 'Example: 1.0.0',
            },
        },
        interference: 'catalog',
    },
    {
        text: 'SSL',
        content: {
            verifySslCertificatesOfServices: {
                value: false,
                question: 'Verify SSL certificates of services:',
            },
            protocol: {
                value: 'TLSv1.2',
                question: 'The TLS protocol version used by Zowe API ML Discovery Service:',
            },
            keyAlias: {
                value: '',
                question: 'The alias used to address the private key in the keystore',
                tooltip: 'Example: localhost',
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
                type: 'password',
                tooltip: 'password',
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
                tooltip: 'Example: keystore/localhost.keystore.p12',
            },
            keyStorePassword: {
                value: '',
                question: 'The password used to unlock the keystore:',
                type: 'password',
                tooltip: 'Your keystore password',
            },
            keyStoreType: {
                value: 'PKCS12',
                question: 'Type of the keystore:',
                options: ['PKCS12', 'JKS', 'JCEKS', 'JCECCAKS', 'JCERACFKS', 'JCECCARACFKS', 'JCEHYBRIDRACFKS'],
            },
            trustStore: {
                value: '',
                question: 'The truststore file used to keep other parties public keys and certificates:',
                tooltip: 'Example: keystore/localhost.truststore.p12',
            },
            trustStorePassword: {
                value: '',
                question: 'The password used to unlock the truststore:',
                type: 'password',
                tooltip: 'Your truststore password.',
            },
            trustStoreType: {
                value: 'PKCS12',
                question: 'Truststore type:',
                options: ['PKCS12', 'JKS', 'JCEKS', 'JCECCAKS', 'JCERACFKS', 'JCECCARACFKS', 'JCEHYBRIDRACFKS'],
            },
        },
    },
];
