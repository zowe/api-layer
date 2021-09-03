// eslint-disable-next-line import/prefer-default-export
export const staticSpecificCategories = [
    {
        text: 'URL for Static',
        content: {
            instanceBaseUrls: {
                value: '',
                question: 'The base URL of the instance (the consistent part of the web address):',
            },
        },
        multiple: false,
        noKey: true,
    },
    {
        text: 'Routes for Static & Node',
        content: {
            gatewayUrl: {
                value: '',
                question: 'The portion of the gateway URL which is replaced by the serviceUrl path part:',
            },
            serviceRelativeUrl: {
                value: '',
                question: 'A portion of the service instance URL path which replaces the gatewayUrl part:',
            },
        },
        multiple: true,
    },
    {
        text: 'Catalog info',
        content: {
            catalogUiTileId: {
                value: '',
                question: 'The id of the catalog tile:',
            },
        },
    },
    {
        text: 'Catalog UI Tiles',
        content: {
            title: {
                value: '',
                question: 'The title of the API services product family:',
            },
            description: {
                value: '',
                question: 'The detailed description of the API Catalog UI dashboard tile:',
            },
        },
        indentationDependency: 'catalogUiTileId',
    },
];
