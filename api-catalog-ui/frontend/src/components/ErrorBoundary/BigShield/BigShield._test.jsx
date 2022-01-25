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
import * as enzyme from 'enzyme';
import BigShield from './BigShield';

// This test is disabled because it is always hanging as pending
// ignoring with xit does not work for some reason

const Child = () => {
    throw new Error('error');
};

const pauseErrorLogging = (codeToRun) => {
    const logger = console.error;
    console.error = () => {
        /* This is intentional */
    };

    codeToRun();

    console.error = logger;
};
describe('>>> BigShield component tests', () => {
    xit('Should catches error and renders message', () => {
        const errorMessage =
            'An unexpected browser error occurredYou are seeing this page because an unexpected error occurred while rendering your page.The Dashboard is broken, you cannot navigate away from this page.Display the error stackDisplay the component stack\n' +
            '    in Child (at BigShield._test.jsx:27)\n' +
            '    in BigShield (created by WrapperComponent)\n' +
            '    in WrapperComponent';
        pauseErrorLogging(() => {
            const wrapper = enzyme.mount(
                <BigShield>
                    <Child />
                </BigShield>
            );
            expect(wrapper.text()).toBe(errorMessage);
        });
    });
});
