/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { defaultMicronaut, defaultNode, defaultPJE, defaultSpring } from './wizard_defaults';

/**
 * Define which categories each enabler needs. Properties indentation and multiple can be used here as well.(override)
 */
export const enablerData = [
    {
        text: 'Plain Java Enabler',
        categories: [
            { name: 'Basic info', nav: 'Basics' },
            { name: 'Description', nav: 'Basics' },
            { name: 'Base URL', nav: 'Basics' },
            { name: 'IP address info', nav: 'IP info' },
            { name: 'Prefer IP address', nav: 'IP info' },
            { name: 'URL' },
            { name: 'Discovery Service URL', indentation: 'discoveryServiceUrls', multiple: true },
            { name: 'Routes', indentation: 'routes', minions: { 'API Info': ['gatewayUrl'] } },
            { name: 'Authentication', indentation: 'authentication' },
            { name: 'API Info', indentation: 'apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'catalog/tile' },
            { name: 'SSL', indentation: 'ssl' },
        ],
        defaults: defaultPJE,
    },
    {
        text: 'Spring Enabler',
        categories: [
            { name: 'Spring', indentation: 'spring/application', nav: 'Basics' },
            { name: 'Enable', indentation: 'apiml', nav: 'Basics' },
            { name: 'Basic info', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Description', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Scheme info', indentation: 'apiml/service' },
            { name: 'Base URL', indentation: 'apiml/service', nav: 'IP & URL' },
            { name: 'IP address info', indentation: 'apiml/service', nav: 'IP & URL' },
            { name: 'URL', indentation: 'apiml/service', nav: 'IP & URL' },
            { name: 'Discovery Service URL', indentation: 'apiml/service/discoveryServiceUrls', multiple: true },
            {
                name: 'Routes',
                indentation: 'apiml/service/routes',
                multiple: true,
                minions: { 'API Info': ['gatewayUrl'] },
            },
            { name: 'API Info', indentation: 'apiml/service/apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'apiml/service/catalog/tile', nav: 'Catalog' },
            { name: 'Authentication', indentation: 'apiml/service/authentication', nav: 'Auth & SSL' },
            { name: 'SSL', indentation: 'apiml/service/ssl', nav: 'Auth & SSL' },
        ],
        defaults: defaultSpring,
    },
    {
        text: 'Micronaut Enabler',
        categories: [
            { name: 'Prefer IP address', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Basic info', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Description', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Discovery Service URL', indentation: 'apiml/service', nav: 'Scheme Info' },
            { name: 'Scheme info', indentation: 'apiml/service', nav: 'Scheme Info' },
            { name: 'Base URL', indentation: 'apiml/service', nav: 'URL' },
            { name: 'URL', indentation: 'apiml/service', nav: 'URL' },
            {
                name: 'Routes',
                indentation: 'apiml/service/routes',
                multiple: true,
                minions: { 'API Info': ['gatewayUrl'] },
            },
            { name: 'API Info for Micronaut', indentation: 'apiml/service/apiInfo', nav: 'API info', multiple: true },
            {
                name: 'Catalog',
                indentation: 'apiml/service/catalog',
                nav: 'Catalog configuration',
                multiple: true,
                arrIndent: 'tile',
            },
            { name: 'SSL detailed', indentation: 'apiml/service/ssl', nav: 'SSL', multiple: true },
            { name: 'Micronaut', indentation: 'micronaut/application', nav: 'Micronaut configuration' },
            { name: 'Micronaut ports', indentation: 'micronaut/server', nav: 'Micronaut configuration' },
            { name: 'Micronaut SSL enable', indentation: 'micronaut/ssl', nav: 'Micronaut SSL configuration' },
            {
                name: 'Micronaut SSL key-store',
                indentation: 'micronaut/ssl/key-store',
                nav: 'Micronaut SSL configuration',
            },
            { name: 'Micronaut SSL key', indentation: 'micronaut/ssl/key', nav: 'Micronaut SSL configuration' },
            {
                name: 'Micronaut SSL trust-store',
                indentation: 'micronaut/ssl/trust-store',
                nav: 'Micronaut configuration',
            },
            { name: 'Micronaut config', indentation: 'micronaut/ssl', nav: 'Micronaut configuration' },
            { name: 'Micronaut Eureka', indentation: 'eureka/client/serviceUrl', nav: 'Micronaut management' },
            {
                name: 'Micronaut management base-path',
                indentation: 'management/endpoints/web',
                nav: 'Micronaut management',
            },
            {
                name: 'Micronaut management exposure',
                indentation: 'management/endpoints/web/exposure',
                nav: 'Micronaut management',
            },
            {
                name: 'Micronaut management health',
                indentation: 'management/endpoints/health/defaults',
                nav: 'Micronaut management',
            },
        ],
        defaults: defaultMicronaut,
    },
    {
        text: 'Node JS Enabler',
        categories: [
            { name: 'Eureka', indentation: 'eureka', nav: 'Basics' },
            { name: 'Instance', indentation: 'instance', nav: 'Instance' },
            { name: 'Instance port', indentation: 'instance/port', nav: 'Instance ports' },
            { name: 'Instance security port', indentation: 'instance/securePort', nav: 'Instance ports' },
            { name: 'Data center info', indentation: 'instance/dataCenterInfo', nav: 'Instance info' },
            { name: 'Metadata', indentation: 'instance/metadata', nav: 'Instance metadata' },
            { name: 'SSL for Node', indentation: 'ssl', nav: 'SSL' },
        ],
        defaults: defaultNode,
    },
    {
        text: 'Static Onboarding',
        categories: [
            { name: 'Basic info', nav: 'Basics', inArr: true },
            { name: 'Description', nav: 'Basics', inArr: true },
            { name: 'Catalog info', nav: 'Basics', inArr: true },
            { name: 'Service info', nav: 'Basics', inArr: true },
            {
                name: 'URL for Static',
                indentation: 'instanceBaseUrls',
                multiple: true,
                nav: 'URL',
                inArr: true,
            },
            { name: 'URL', nav: 'URL', inArr: true },
            {
                name: 'Routes for Static & Node',
                nav: 'Routes',
                indentation: 'routes',
                multiple: true,
                inArr: true,
                minions: { 'API Info': ['gatewayUrl'] },
            },
            { name: 'Authentication', indentation: 'authentication', inArr: true },
            { name: 'API Info', indentation: 'apiInfo', multiple: true, inArr: true },
            { name: 'Catalog UI Tiles', indentation: 'catalogUiTiles' },
        ],
    },
    {
        text: 'Direct Call to Eureka',
    },
];
