/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import basicSsl from '@vitejs/plugin-basic-ssl';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

// to support redirecting from https://localhost:3000 to /apicatalog/ui/v1
const redirectMissingTrailingSlashPlugin = (basePath) => {
    const pathWithoutSlash = basePath.slice(0, -1);
    return {
        name: 'redirect-missing-trailing-slash',
        configureServer: (server) => {
            server.middlewares.use((req, res, next) => {
                const [path, search = ''] = (req.url || '').split('?');
                if (path === pathWithoutSlash) {
                    res.writeHead(302, {
                        Location: basePath + (search ? `?${search}` : ''),
                    });
                    return res.end();
                }
                next();
            });
        },
    }
};

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const gatewayUrl = env.VITE_GATEWAY_URL || 'https://localhost:10010';
    console.log('gatewayUrl', gatewayUrl);
    const base = '/apicatalog/ui/v1/';
    return {
        root: __dirname,
        plugins: [
            redirectMissingTrailingSlashPlugin(base),
            react(),
            basicSsl(),
            nodePolyfills({
                include: ['buffer', 'process', 'stream', 'util', 'url', 'querystring'],
                globals: {
                    Buffer: true,
                    global: true,
                    process: true,
                },
            }),
        ],
        base,
        build: {
            outDir: 'build',
            sourcemap: false,
            assetsDir: 'static',
        },
        server: {
            port: 3000,
            strictPort: true,
            https: true,
            proxy: {
                '^/(?!apicatalog/ui).*': {
                    target: gatewayUrl,
                    secure: false,
                    changeOrigin: true,
                    cookieDomainRewrite: 'localhost',
                    cookiePathRewrite: '/',
                    headers: {
                        Origin: gatewayUrl,
                    },
                },
            },
        },
        optimizeDeps: {
            include: ['swagger-ui-react'],
        },
        define: {
            'process.env': {},
        },
    };
});

