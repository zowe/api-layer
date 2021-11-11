// eslint-disable-next-line import/prefer-default-export
export const springSpecificCategories = [
    {
        text: 'Enable',
        content: {
            enabled: {
                value: false,
                question: 'Service should automatically register with API ML discovery service',
            },
            enableUrlEncodedCharacters: {
                value: false,
                question: 'Service requests the API ML GW to receive encoded characters in the URL',
            },
        },
    },
    {
        text: 'Spring',
        content: {
            name: {
                value: '',
                question: 'This parameter has to be the same as the service ID you are going to provide',
            },
        },
    },
];
