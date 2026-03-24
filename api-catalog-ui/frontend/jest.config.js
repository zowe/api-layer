/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/** @type {import('jest').Config} */
const config = {
    testEnvironment: 'jsdom',
    testEnvironmentOptions: {
        customExportConditions: ['node', 'require', 'default'],
    },
    transformIgnorePatterns: [
        'node_modules/(?!(swagger-client|react-syntax-highlighter|enzyme|cheerio|yaml|uuid|graphiql|@graphiql)/)',
    ],
    collectCoverageFrom: [
        'src/App.{jsx,js}',
        'src/**/*.{jsx,js}',
        'src/**/reducers/*.{jsx,js}',
        '!src/index.jsx',
        '!src/responsive-tests/**',
        '!cypress/*',
    ],
    moduleNameMapper: {
        '^graphiql$': '<rootDir>/src/__mocks__/graphiql.js',
        '^swagger-ui-react$': '<rootDir>/node_modules/swagger-ui-react/index.cjs',
        '^react-syntax-highlighter/dist/esm/(.*)$': '<rootDir>/node_modules/react-syntax-highlighter/dist/cjs/$1',
        '^swagger-client/es/(.*)$': '<rootDir>/node_modules/swagger-client/lib/$1',
        '^react-redux$': '<rootDir>/node_modules/react-redux/dist/cjs/index.js',
        '^redux-observable$': '<rootDir>/node_modules/redux-observable/dist/cjs/redux-observable.cjs',
        '^@reduxjs/toolkit$': '<rootDir>/node_modules/@reduxjs/toolkit/dist/cjs/index.js',
        '^#apg-lite$': '<rootDir>/node_modules/apg-lite/lib/parser.js',
        '^#swagger-ui$': '<rootDir>/node_modules/swagger-ui-react/swagger-ui.js',
        '^#buffer':
            '<rootDir>/node_modules/@swagger-api/apidom-reference/src/util/polyfills/buffer/standard-import.cjs',
        '@swagger-api/apidom-reference/configuration/empty':
            '<rootDir>/node_modules/@swagger-api/apidom-reference/src/configuration/empty.cjs',
        '@swagger-api/apidom-reference/parse/parsers/binary':
            '<rootDir>/node_modules/@swagger-api/apidom-reference/src/parse/parsers/binary/index-node.cjs',
        '@swagger-api/apidom-reference/resolve/strategies/openapi-3-1':
            '<rootDir>/node_modules/@swagger-api/apidom-reference/src/resolve/strategies/openapi-3-1/index.cjs',
        '@swagger-api/apidom-reference/dereference/strategies/openapi-3-1':
            '<rootDir>/node_modules/@swagger-api/apidom-reference/src/dereference/strategies/openapi-3-1/index.cjs',
        '@swagger-api/apidom-json-pointer/modern':
            '<rootDir>/node_modules/@swagger-api/apidom-json-pointer/src/index.cjs',
        '@swaggerexpert/json-pointer/evaluate/realms/apidom':
            '<rootDir>/node_modules/@swaggerexpert/json-pointer/cjs/evaluate/realms/apidom/index.cjs',
        'cheerio/lib/utils': '<rootDir>/node_modules/cheerio',
    },
    transform: {
        '^.+\\.(js|jsx)$': 'babel-jest',
        '^.+\\.css$': '<rootDir>/jest.transform.css.js',
        '^.+\\.scss$': '<rootDir>/jest.transform.css.js',
        '^(?!.*\\.(js|jsx|css|scss|json)$)': '<rootDir>/jest.transform.file.js',
    },
    setupFiles: ['./jest.polyfills.js'],
    setupFilesAfterEnv: ['@testing-library/jest-dom', '<rootDir>/src/setupTests.js'],
};

module.exports = config;
