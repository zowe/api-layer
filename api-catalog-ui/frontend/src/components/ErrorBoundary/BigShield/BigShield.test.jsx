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
import React from 'react';
import { render } from 'react-dom';
import { act } from 'react-dom/test-utils';
import BigShield from './BigShield';

const Child = () => {
    // eslint-disable-next-line no-throw-literal
    throw 'error';
};

describe('>>> BigShield component tests', () => {
    it('Should catches error and renders message', () => {
        const errorMessage =
            'An unexpected browser error occurredYou are seeing this page because an unexpected error occurred while rendering your page.The Dashboard is broken, you cannot navigate away from this page.Display the error stackDisplay the component stack\n' +
            '    at Child\n ' +
            '   at BigShield';
        const container = document.createElement('div');
        act(() => {
            render(
                <BigShield>
                    <Child />
                </BigShield>,
                container
            );
        });
        expect(container.textContent).toMatch(errorMessage);
    });
});
