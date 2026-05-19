/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

import AuthRoute from './AuthRoute';

describe('>>> AuthRoute component tests', () => {
    it('should contain a Spinner component when waiting for authentication result', () => {
        const wrapper = mount(
            <MemoryRouter>
                <AuthRoute>
                    <div>test</div>
                </AuthRoute>
            </MemoryRouter>
        );
        expect(wrapper.find('Spinner')).toExist();
    });

    it('should render children when authenticated', async () => {
        const mockResponse = { ok: true, text: () => Promise.resolve() };
        jest.spyOn(global, 'fetch').mockImplementation(() => Promise.resolve(mockResponse));

        const wrapper = mount(
            <MemoryRouter>
                <AuthRoute>
                    <div data-testid="child">child</div>
                </AuthRoute>
            </MemoryRouter>
        );

        // awaits response from mocked fetch call
        await act(async () => {
            await new Promise(process.nextTick);
            wrapper.update();
        });
        expect(wrapper.find('[data-testid="child"]').exists()).toBe(true);
    });
});
