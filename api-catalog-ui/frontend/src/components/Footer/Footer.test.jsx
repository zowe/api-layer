/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Footer from './Footer';

describe('>>> Footer component tests', () => {
    const versionInfo = {
        zowe: { version: '3.5.0', buildNumber: '10407', commitHash: 'deadbee' },
        apiml: { version: '3.5.19', buildNumber: '402', commitHash: 'cafef00d' },
    };

    beforeEach(() => {
        global.fetch = jest.fn();
    });

    afterEach(() => {
        global.fetch.mockRestore();
    });

    it('should display the version from the Gateway /version endpoint', async () => {
        global.fetch.mockImplementation(() =>
            Promise.resolve({
                ok: true,
                json: () => Promise.resolve(versionInfo),
            })
        );
        render(<Footer />);
        await waitFor(() =>
            expect(screen.getByText('API ML Version 3.5.19 build # 402')).toBeInTheDocument()
        );
        expect(global.fetch).toHaveBeenCalledWith('https://localhost:10010/gateway/version');
    });

    it('should fall back to the build time version when the Gateway is not reachable', async () => {
        global.fetch.mockImplementation(() => Promise.reject(new Error('unreachable')));
        render(<Footer />);
        await waitFor(() => expect(global.fetch).toHaveBeenCalled());
        expect(screen.getByText('test build info')).toBeInTheDocument();
    });

    it('should fall back to the build time version when the payload carries no version', async () => {
        global.fetch.mockImplementation(() =>
            Promise.resolve({
                ok: true,
                json: () => Promise.resolve({}),
            })
        );
        render(<Footer />);
        await waitFor(() => expect(global.fetch).toHaveBeenCalled());
        expect(screen.getByText('test build info')).toBeInTheDocument();
    });
});
