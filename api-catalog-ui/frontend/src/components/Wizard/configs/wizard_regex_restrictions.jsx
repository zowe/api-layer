/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export const wizRegex = {
    gatewayUrl: { value: '^(/[a-z]+\\/v\\d+)$', tooltip: 'Format: /api/vX, Example: /api/v1' },
    version: { value: '^(\\d+)\\.(\\d+)\\.(\\d+)$', tooltip: 'Semantic versioning expected, example: 1.0.7' },
    ipAddress: {
        value: '^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$',
        tooltip: 'IP Address v4 expected, example: 127.0.0.1',
    },
    validRelativeUrl: {
        value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
        tooltip: 'The relative URL has to be valid, example: /application/info',
    },
    noWhiteSpaces: { value: '^[a-zA-Z1-9]+$', tooltip: 'Only alphanumerical values with no whitespaces are accepted' },
};
