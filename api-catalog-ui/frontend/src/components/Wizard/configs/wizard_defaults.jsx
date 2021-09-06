/**
 * Define default values for different enablers
 */
// eslint-disable-next-line import/prefer-default-export
export const defaultSpring = {
    Spring: { name: { value: '${apiml.service.serviceId}', hide: true } },
    'Scheme info': { scheme: { value: 'https' }, contextPath: { value: '/${apiml.service.serviceId}' } },
    'IP address info': {
        baseUrl: { value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}' },
    },
    URL: {
        homePageRelativeUrl: { value: '${apiml.service.contextPath}/' },
        statusPageRelativeUrl: { value: '${apiml.service.contextPath}/' },
        healthCheckRelativeUrl: { value: '${apiml.service.contextPath}/' },
    },
    Routes: {
        serviceUrl: { value: '${apiml.service.contextPath}', hide: true },
    },
    'API Info': {
        swaggerUrl: {
            value:
                '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
            hide: true,
        },
    },
};
export const defaultNode = {
    Eureka: {
        maxRetries: { value: 30, hide: true },
        requestRetryDelay: { value: 1000, hide: true },
        registryFetchInterval: { value: 5, hide: true },
    },
    'API Info shorter': {
        gatewayUrl: { value: '${routes.gatewayUrl}' },
    },
    Instance: {
        app: { value: '${serviceId}' },
        vipAddress: { value: '${serviceId}' },
        homePageUrl: { value: '${homePageRelativeUrl}' },
        secureVipAddress: { value: '${serviceId}' },
    },
    Metadata: {
        'apiml.routes.api_v1.gatewayUrl': { value: '${routes.gatewayUrl}' },
        'apiml.routes.api_v1.serviceUrl': { value: '${routes.serviceUrl}' },
        'apiml.apiInfo.0.gatewayUrl': { value: '${routes.gatewayUrl}' },
    },
};
export const defaultMicronaut = {
    Micronaut: {
        name: { value: '${apiml.service.serviceId}' },
    },
    'Micronaut ports': {
        port: { value: '${apiml.service.port}' },
        'context-path': { value: '/${apiml.service.serviceId}' },
    },
    'Micronaut SSL key-store': {
        password: { value: '${apiml.service.ssl[0].keyPassword}' },
        type: { value: '${apiml.service.ssl[0].keyStoreType}' },
        path: { value: 'file:${apiml.service.ssl[0].keyStore}' },
    },
    'Micronaut SSL key': {
        alias: { value: '${apiml.service.ssl[0].keyAlias}' },
        password: { value: '${apiml.service.ssl[0].keyPassword}' },
    },
    'Micronaut SSL trust-store': {
        password: { value: '${apiml.service.ssl[0].trustStorePassword}' },
        path: { value: 'file:${apiml.service.ssl[0].trustStore}' },
        type: { value: '${apiml.service.ssl[0].trustStoreType}' },
    },
    'Micronaut config': {
        port: { value: '${apiml.service.port}' },
        ciphers: { value: '${apiml.service.ssl[0].ciphers}' },
        protocol: { value: '${apiml.service.ssl[0].protocol}' },
    },
    'Base URL': {
        baseUrl: { value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}' },
    },
    'Scheme info': {
        contextPath: { value: '/${apiml.service.serviceId}' },
    },
    URL: {
        homePageRelativeUrl: { value: '${apiml.service.contextPath}' },
        statusPageRelativeUrl: { value: '${apiml.service.contextPath}' },
        healthCheckRelativeUrl: { value: '${apiml.service.contextPath}' },
    },
    'API Info for Micronaut': {
        gatewayUrl: { value: '${apiml.service.routes.gatewayUrl}' },
    },
};
