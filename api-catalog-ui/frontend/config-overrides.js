/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
module.exports = {
    webpack(config, env) {
        // New config, e.g. config.plugins.push...

        config.module.rules = [
            ...config.module.rules,
            {
                test: /\.html$/i,
                use: 'html-loader',
            },
            {
                resolve: {
                    fallback: { querystring: require.resolve('querystring-es3'), url: require.resolve('url/') },
                },
            },
        ];

        return config;
    },
    jest: (config) => {
        config.transformIgnorePatterns = ['/node_modules/?!(swagger-ui-react|swagger-client|react-syntax-highlighter)'];
        config.collectCoverageFrom = [
            'src/App.{jsx,js}',
            'src/**/*.{jsx,js}',
            'src/**/reducers/*.{jsx,js}',
            '!src/index.js',
            '!src/responsive-tests/**',
            '!cypress/*',
        ];
        config.moduleNameMapper = {
            '^#apg-lite$': '<rootDir>/node_modules/apg-lite/lib/parser.js',
            '^#swagger-ui$': '<rootDir>/node_modules/swagger-ui-react/swagger-ui.js',
            '^#buffer':
                '<rootDir>/node_modules/@swagger-api/apidom-reference/cjs/util/polyfills/buffer/standard-import.cjs',
            '@swagger-api/apidom-reference/configuration/empty':
                '<rootDir>/node_modules/@swagger-api/apidom-reference/cjs/configuration/empty.cjs',
            '@swagger-api/apidom-reference/parse/parsers/binary':
                '<rootDir>/node_modules/@swagger-api/apidom-reference/cjs/parse/parsers/binary/index-node.cjs',
            '@swagger-api/apidom-reference/resolve/strategies/openapi-3-1':
                '<rootDir>/node_modules/@swagger-api/apidom-reference/cjs/resolve/strategies/openapi-3-1/index.cjs',
            '@swagger-api/apidom-reference/dereference/strategies/openapi-3-1':
                '<rootDir>/node_modules/@swagger-api/apidom-reference/cjs/dereference/strategies/openapi-3-1/index.cjs',
            'cheerio/lib/utils': '<rootDir>/node_modules/cheerio',
        };
        config.setupFiles = ['./jest.polyfills.js'];
        return config;
    },
};
