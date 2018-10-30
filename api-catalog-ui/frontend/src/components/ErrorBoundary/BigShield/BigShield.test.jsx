/* eslint-disable no-undef */
import React from 'react';
import * as enzyme from 'enzyme';
import BigShield from './BigShield';

const Child = () => {
    throw 'error';
};

const pauseErrorLogging = codeToRun => {
    const logger = console.error;
    console.error = () => {};

    codeToRun();

    console.error = logger;
};
describe('>>> BigShield component tests', () => {
    it('Should catches error and renders message', () => {
        const errorMessage = "An unexpected browser error occurredYou are seeing this page because an unexpected error occurred while rendering your page.The Dashboard is broken, you cannot navigate away from this page.Display the error stackDisplay the component stack\n" +
            "    in Child (at BigShield.test.jsx:27)\n" +
            "    in BigShield (created by WrapperComponent)\n" +
            "    in WrapperComponent";
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
