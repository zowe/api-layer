/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

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
                    fallback: { querystring: require.resolve('querystring-es3') },
                },
            },
        ];

        config.plugins.push(
            new HtmlWebpackPlugin({
                title: 'Caching',
            })
        );

        config.output = {
            filename: '[name].[contenthash].js',
            path: path.resolve(__dirname, 'dist'),
            clean: true,
        };

        return config;
    },
    jest: (config) => {
        config.transformIgnorePatterns = ['/node_modules/?!(swagger-ui-react/swagger-ui-es-bundle-core.js)'];
        config.collectCoverageFrom = [
            'src/App.{jsx,js}',
            'src/**/*.{jsx,js}',
            'src/**/reducers/*.{jsx,js}',
            '!src/index.js',
            '!src/responsive-tests/**',
            '!cypress/*',
        ];
        return config;
    },
};
