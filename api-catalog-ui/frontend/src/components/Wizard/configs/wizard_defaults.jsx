/**
 * Define default values for different enablers
 */
// eslint-disable-next-line import/prefer-default-export
export const defaultSpring = {
    Spring: { name: '${apiml.service.serviceId}' },
    'Scheme info': { scheme: 'https', contextPath: '/${apiml.service.serviceId}' },
    'IP address info': { baseUrl: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}' },
    URL: {
        homePageRelativeUrl: '${apiml.service.contextPath}',
        statusPageRelativeUrl: '${apiml.service.contextPath}',
        healthCheckRelativeUrl: '${apiml.service.contextPath}',
    },
    Routes: {
        serviceUrl: '${apiml.service.contextPath}',
    },
    'API Info': {
        swaggerUrl:
            '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
    },
};
export const defaultNode = {
    Eureka: {
        maxRetries: 30,
        requestRetryDelay: 1000,
        registryFetchInterval: 5,
    },
    'API Info shorter': {
        gatewayUrl: '${routes.gatewayUrl}',
    },
    Instance: {
        app: '${serviceId}',
        vipAddress: '${serviceId}',
        homePageUrl: '${homePageRelativeUrl}',
        secureVipAddress: '${serviceId}',
    },
    Metadata: {
        'apiml.routes.api_v1.gatewayUrl': '${routes.gatewayUrl}',
        'apiml.routes.api_v1.serviceUrl': '${routes.serviceUrl}',
        'apiml.apiInfo.0.gatewayUrl': '${routes.gatewayUrl}',
    },
};
export const defaultMicronaut = {
    Micronaut: {
        name: '${apiml.service.serviceId}',
    },
    'Micronaut ports': {
        port: '${apiml.service.port}',
        'context-path': '/${apiml.service.serviceId}',
    },
    'Micronaut SSL key-store': {
        password: '${apiml.service.ssl[0].keyPassword}',
        type: '${apiml.service.ssl[0].keyStoreType}',
        path: 'file:${apiml.service.ssl[0].keyStore}',
    },
    'Micronaut SSL key': {
        alias: '${apiml.service.ssl[0].keyAlias}',
        password: '${apiml.service.ssl[0].keyPassword}',
    },
    'Micronaut SSL trust-store': {
        password: '${apiml.service.ssl[0].trustStorePassword}',
        path: 'file:${apiml.service.ssl[0].trustStore}',
        type: '${apiml.service.ssl[0].trustStoreType}',
    },
    'Micronaut config': {
        port: '${apiml.service.port}',
        ciphers: '${apiml.service.ssl[0].ciphers}',
        protocol: '${apiml.service.ssl[0].protocol}',
    },
    'Base URL': {
        baseUrl: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}',
    },
    'Scheme info': {
        contextPath: '/${apiml.service.serviceId}',
    },
    URL: {
        homePageRelativeUrl: '${apiml.service.contextPath}',
        statusPageRelativeUrl: '${apiml.service.contextPath}',
        healthCheckRelativeUrl: '${apiml.service.contextPath}',
    },
    'API Info for Micronaut': {
        gatewayUrl: '${apiml.service.routes.gatewayUrl}',
    },
};
