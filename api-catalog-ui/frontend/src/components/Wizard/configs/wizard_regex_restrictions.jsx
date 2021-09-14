// eslint-disable-next-line import/prefer-default-export
export const wizRegex = {
    gatewayUrl: { value: '^(/[a-z]+\\/v\\d+)$', tooltip: 'Format: /api/vX, Example: /api/v1' },
    version: { value: '^(\\d+)\\.(\\d+)\\.(\\d+)$', tooltip: 'Semantic versioning expected, example: 1.0.7' },
    validRelativeUrl: {
        value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
        tooltip: 'The relative URL has to be valid, example: /application/info',
    },
    noWhiteSpaces: { value: '^[a-zA-Z1-9]+$', tooltip: 'Only alphanumerical values with no whitespaces are accepted' },
};
