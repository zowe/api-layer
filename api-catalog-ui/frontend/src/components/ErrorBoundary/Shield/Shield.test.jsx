/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable no-console */
import { React, act } from 'react';
import { createRoot } from 'react-dom/client';
import Shield from './Shield';

const Child = () => {
    // eslint-disable-next-line no-throw-literal
    throw 'error';
};

describe('>>> Shield component tests', () => {
    it('Should catches error and renders message', () => {
        const errorMessage = 'Display the error stack';
        const container = document.createElement('div');
        act(() => {
            const root = createRoot(container);
            root.render(
                <Shield>
                    <Child />
                </Shield>,
                container
            );
        });
        expect(container.textContent).toBe(errorMessage);
    });
});
