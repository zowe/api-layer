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

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const gatewayUrl = env.VITE_GATEWAY_URL || 'https://localhost:10010';
    console.log('gatewayUrl', gatewayUrl);
    return {
        root: __dirname,
        plugins: [
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
        base: '/apicatalog/ui/v1/',
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
                // REGEX DINAMICA: Intercetta qualsiasi rotta TRANNE le risorse statiche della UI (/apicatalog/ui/...)
                '^/(?!apicatalog/ui).*': {
                    target: gatewayUrl,
                    secure: false,
                    changeOrigin: true,
                    cookieDomainRewrite: 'localhost',
                    cookiePathRewrite: '/',
                    headers: {
                        Origin: gatewayUrl,
                    },
                    configure: (proxy) => {
                        proxy.on('proxyRes', (proxyRes) => {
                            const setCookie = proxyRes.headers['set-cookie'];
                            if (setCookie) {
                                proxyRes.headers['set-cookie'] = setCookie.map(cookie =>
                                    cookie
                                        .replace(/Path=[^;]+;/i, 'Path=/;')
                                        .replace(/Domain=[^;]+;/i, 'Domain=localhost;')
                                        .replace(/SameSite=Lax/i, 'SameSite=None')
                                );
                            }
                        });
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

