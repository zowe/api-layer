import { defaultSpring } from './wizard_defaults';
// eslint-disable-next-line import/prefer-default-export
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
