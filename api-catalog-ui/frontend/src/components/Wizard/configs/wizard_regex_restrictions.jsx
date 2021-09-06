// eslint-disable-next-line import/prefer-default-export
export const wizRegex = {
    gatewayUrl: '^(api\\/v\\d+)$',
    version: '^(\\d+)\\.(\\d+)\\.(\\d+)',
    alphanumeric: '^[a-z]+$',
    validRelativeUrl: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
    noWhiteSpaces: '^[a-zA-Z1-9]+$',
};
