/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/**
 * Define default values for different enablers
 */

export const defaultPJE = {
    SSL: { protocol: { value: 'TLSv1.2', hide: true } },
};
export const defaultSpring = {
    Spring: { name: { value: '${apiml.service.serviceId}', hide: true } },
    'Scheme info': { scheme: { value: 'https' }, contextPath: { value: '/${apiml.service.serviceId}', hide: true } },
    'IP address info': {
        baseUrl: { value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}' },
    },
    URL: {
        homePageRelativeUrl: { value: '${apiml.service.contextPath}/' },
        statusPageRelativeUrl: { value: '${apiml.service.contextPath}/' },
        healthCheckRelativeUrl: { value: '${apiml.service.contextPath}/' },
    },
    'API Info': {
        swaggerUrl: {
            value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
            hide: true,
        },
        graphqlUrl: {
            value: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
            hide: true,
        },
    },
    SSL: { protocol: { value: 'TLSv1.2', hide: true } },
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
        app: { value: '${serviceId}', hide: true },
        vipAddress: { value: '${serviceId}', hide: true },
        homePageUrl: { value: '${homePageRelativeUrl}' },
        secureVipAddress: { value: '${serviceId}', hide: true },
    },
    Metadata: {
        'apiml.routes.api_v1.gatewayUrl': { value: '${routes.gatewayUrl}', hide: true },
        'apiml.routes.api_v1.serviceUrl': { value: '${routes.serviceUrl}', hide: true },
        'apiml.apiInfo.0.gatewayUrl': { value: '${routes.gatewayUrl}', hide: true },
    },
};
export const defaultMicronaut = {
    Micronaut: {
        name: { value: '${apiml.service.serviceId}' },
    },
    'Micronaut ports': {
        port: { value: '${apiml.service.port}' },
        'context-path': { value: '/${apiml.service.serviceId}', hide: true },
    },
    'Micronaut SSL key-store': {
        password: { value: '${apiml.service.ssl[0].keyPassword}', hide: true },
        type: { value: '${apiml.service.ssl[0].keyStoreType}', hide: true },
        path: { value: 'file:${apiml.service.ssl[0].keyStore}', hide: true },
    },
    'Micronaut SSL key': {
        alias: { value: '${apiml.service.ssl[0].keyAlias}', hide: true },
        password: { value: '${apiml.service.ssl[0].keyPassword}', hide: true },
    },
    'Micronaut SSL trust-store': {
        password: { value: '${apiml.service.ssl[0].trustStorePassword}', hide: true },
        path: { value: 'file:${apiml.service.ssl[0].trustStore}', hide: true },
        type: { value: '${apiml.service.ssl[0].trustStoreType}', hide: true },
    },
    'Micronaut config': {
        port: { value: '${apiml.service.port}', hide: true },
        ciphers: { value: '${apiml.service.ssl[0].ciphers}', hide: true },
        protocol: { value: '${apiml.service.ssl[0].protocol}', hide: true },
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
    'SSL detailed': { protocol: { value: 'TLSv1.2', hide: true } },
};
