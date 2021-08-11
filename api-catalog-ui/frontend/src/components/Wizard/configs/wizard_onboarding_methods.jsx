import { defaultSpring } from './wizard_defaults';

/**
 * Define which categories each enabler needs. Properties indentation and multiple can be used here as well.(override)
 */
// eslint-disable-next-line import/prefer-default-export
export const enablerData = [
    {
        text: 'Plain Java Enabler',
        categories: [
            { name: 'Basic info' },
            { name: 'IP address info' },
            { name: 'Prefer IP address' },
            { name: 'URL' },
            { name: 'Discovery Service URL', multiple: true },
            { name: 'Routes', indentation: 'routes' },
            { name: 'Authentication', indentation: 'authentication' },
            { name: 'API Info', indentation: 'apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'catalog/tiles' },
            { name: 'SSL', indentation: 'ssl' },
        ],
    },
    {
        text: 'Spring Enabler',
        categories: [
            { name: 'Spring', indentation: 'spring/application', nav: 'Basics' },
            { name: 'Enable', indentation: 'apiml', nav: 'Basics' },
            { name: 'Basic info', indentation: 'apiml/service', nav: 'Basics' },
            { name: 'Scheme info', indentation: 'apiml/service' },
            { name: 'IP address info', indentation: 'apiml/service', nav: 'IP & URL' },
            { name: 'URL', indentation: 'apiml/service', nav: 'IP & URL' },
            { name: 'Discovery Service URL', indentation: 'apiml/service/discoveryServiceUrls', multiple: true },
            { name: 'Routes', indentation: 'apiml/service/routes', multiple: true },
            { name: 'API Info', indentation: 'apiml/service/apiInfo', multiple: true },
            { name: 'Catalog', indentation: 'apiml/service/catalog/tiles', nav: 'Catalog' },
            { name: 'Authentication', indentation: 'apiml/service/authentication', nav: 'Auth & SSL' },
            { name: 'SSL', indentation: 'apiml/service/ssl', nav: 'Auth & SSL' },
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
