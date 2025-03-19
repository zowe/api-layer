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
import {act} from 'react';
import {createRoot} from 'react-dom/client';
import BigShield from './BigShield';
import {useNavigate} from "react-router";

const Child = () => {
    // eslint-disable-next-line no-throw-literal
    throw 'error';
};
const mockNavigate = jest.fn();
const mockLocation = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
        useLocation: () => mockLocation,
    };
});
describe('>>> BigShield component tests', () => {
    it('Should catches error and renders message', () => {
        const errorMessageMatch = new RegExp(
            'An unexpected browser error occurredYou are seeing this page because an unexpected error occurred while rendering your page.The Dashboard is broken, you cannot navigate away from this page.Display the error stackDisplay the component stack.*'
        );
        const container = document.createElement('div');
        act(() => {
            const root = createRoot(container);
            root.render(
                <BigShield disableButton={true}>
                    <Child />
                </BigShield>
            );
        });
        expect(container.textContent).toMatch(errorMessageMatch);
    });

    it('Should go back to dashboard', () => {
        jest.mock('react-router', () => ({
            ...jest.requireActual('react-router'),
            useNavigate: () => jest.fn(),
        }));

        const container = document.createElement('div');
        act(() => {
            const root = createRoot(container);
            root.render(
                <BigShield disableButton={false}>
                    <Child />
                </BigShield>
            );
        });

        const button = container.querySelector('button');

        expect(button.textContent).toBe('Go to Dashboard');

        act(() => {
            button.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        });

        expect(useNavigate()).toHaveBeenCalledWith('/dashboard');
    });
});
