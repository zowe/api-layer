// eslint-disable-next-line import/prefer-default-export
export const wizRegex = {
    gatewayUrl: '^(/[a-z]+\\/v\\d+)$',
    version: '^(\\d+)\\.(\\d+)\\.(\\d+)',
    validRelativeUrl: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
    noWhiteSpaces: '^[a-zA-Z1-9]+$',
};
