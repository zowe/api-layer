/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { nodePolyfills } from 'vite-plugin-node-polyfills';

export default defineConfig({
    plugins: [
        react(),
        nodePolyfills({
            include: ['buffer', 'process', 'stream', 'util', 'url', 'querystring'],
            globals: {
                Buffer: true,
                global: true,
                process: true,
            },
        }),
    ],
    base: './',
    build: {
        outDir: 'build',
        sourcemap: false,
        assetsDir: 'static',
    },
    server: {
        https: true,
        proxy: {
            '/apicatalog': {
                target: 'https://localhost:10010',
                secure: false,
                changeOrigin: true,
            },
        },
    },
    optimizeDeps: {
        include: ['swagger-ui-react'],
    },
    define: {
        'process.env': {},
    },
});
