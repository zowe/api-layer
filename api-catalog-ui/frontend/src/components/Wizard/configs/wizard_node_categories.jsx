// eslint-disable-next-line import/prefer-default-export
export const nodeSpecificCategories = [
    {
        text: 'API Info for Node',
        content: {
            apiId: {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
            },
            gatewayUrl: {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
            },
            swaggerUrl: {
                value: '',
                question: 'The Http or Https address where the Swagger JSON document is available:',
                optional: true,
            },
        },
    },
    {
        text: 'SSL for Node',
        content: {
            certificate: {
                value: '',
                question: 'Certificate:',
            },
            keyStore: {
                value: '',
                question: 'The keystore file used to store the private key (keyring: set to SAF keyring location):',
            },
            caFile: {
                value: '',
                question: 'Certificate Authority file:',
            },
            keyPassword: {
                value: '',
                question: 'The password associated with the private key:',
            },
        },
    },
    {
        text: 'Eureka',
        content: {
            ssl: {
                value: false,
                question: 'Turn SSL on for Eureka',
            },
            host: {
                value: '',
                question: 'The host to be used:',
            },
            ipAddress: {
                value: '',
                question: 'The IP address to be used:',
            },
            port: {
                value: '',
                question: 'The port to be used:',
            },
            servicePath: {
                value: '',
                question: 'The service path:',
            },
            maxRetries: {
                value: '',
                question: 'The maximum number of retries:',
            },
            requestRetryDelay: {
                value: '',
                question: 'The request retry delay:',
            },
            registryFetchInterval: {
                value: '',
                question: 'The interval for registry interval:',
            },
        },
    },
    {
        text: 'Instance',
        content: {
            app: {
                value: '',
                question: 'App ID:',
            },
            vipAddress: {
                value: '',
                question: 'Virtual IP address:',
            },
            instanceId: {
                value: '',
                question: 'Instance ID:',
            },
            homePageUrl: {
                value: '',
                question: 'The URL of the home page:',
            },
            hostname: {
                value: '',
                question: 'Host name:',
            },
            ipAddr: {
                value: '',
                question: 'IP address:',
            },
            secureVipAddress: {
                value: '',
                question: 'Secure virtual IP address:',
            },
        },
    },
    {
        text: 'Instance port',
        content: {
            $: {
                value: '',
                question: 'Port:',
            },
            '@enabled': {
                value: false,
                question: 'Enable?',
            },
        },
    },
    {
        text: 'Instance security port',
        content: {
            $: {
                value: '',
                question: 'Security port:',
            },
            '@enabled': {
                value: true,
                question: 'Enable?',
            },
        },
    },
    {
        text: 'Data center info',
        content: {
            '@class': {
                value: '',
                question: 'Class:',
            },
            name: {
                value: '',
                question: 'Name:',
            },
        },
    },
    {
        text: 'Metadata',
        content: {
            'apiml.catalog.tile.id': {
                value: '',
                question: 'Tile ID for the API ML catalog:',
            },
            'apiml.catalog.tile.title': {
                value: '',
                question: 'Tile title for the API ML catalog:',
            },
            'apiml.catalog.tile.description': {
                value: '',
                question: 'Tile description for the API ML catalog:',
            },
            'apiml.catalog.tile.version': {
                value: '',
                question: 'Tile version for the API ML catalog:',
            },
            'apiml.routes.api_v1.gatewayUrl': {
                value: '',
                question: 'API gateway URL:',
            },
            'apiml.routes.api_v1.serviceUrl': {
                value: '',
                question: 'API service URL:',
            },
            'apiml.apiInfo.0.apiId': {
                value: '',
                question: 'A unique identifier to the API in the API ML:',
            },
            'apiml.apiInfo.0.gatewayUrl': {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
            },
            'apiml.apiInfo.0.swaggerUrl': {
                value: '',
                question: 'The base path at the API Gateway where the API is available:',
            },
            'apiml.service.title': {
                value: '',
                question: 'Service title:',
            },
            'apiml.service.description': {
                value: '',
                question: 'Service description:',
            },
        },
    },
];
