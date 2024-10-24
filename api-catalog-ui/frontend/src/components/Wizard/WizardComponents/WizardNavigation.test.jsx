/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable react/display-name */
import * as enzyme from 'enzyme';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import WizardNavigation from './WizardNavigation';

jest.mock(
    '../YAML/YAMLVisualizerContainer',
    () =>
        function () {
            const YAMLVisualizerContainerMock = 'YAMLVisualizerContainerMock';
            return <YAMLVisualizerContainerMock />;
        }
);
jest.mock(
    './WizardInputsContainer',
    () =>
        function () {
            const WizardInputsContainer = 'WizardInputsContainerMock';
            return <WizardInputsContainer />;
        }
);
describe('>>> Wizard navigation tests', () => {
    it('should handle category change', async () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const validateInput = jest.fn();

        render(
            <WizardNavigation
                selectedCategory={0}
                inputData={[]}
                navsObj={{ Nav1: {} }}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                validateInput={validateInput}
            />
        );
        await userEvent.click(screen.getByRole('tab'));
        expect(changeWizardCategory).toHaveBeenCalled();
        expect(validateInput).toHaveBeenCalled();
    });
    it('should validate all tabs on YAML tab click', async () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const validateInput = jest.fn();
        const dummyData = [
            {
                text: 'Some Enabler',
                nav: 'Nav #1',
                categories: [
                    { name: 'Category 1', indentation: false },
                    { name: 'Category 2', indentation: false },
                ],
                help: 'Some additional information',
                helpUrl: {
                    title: 'Help',
                    link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
                },
            },
            {
                text: 'Other Enabler',
                nav: 'Nav #2',
                categories: [
                    { name: 'Category 1', indentation: false },
                    { name: 'Category 2', indentation: false },
                ],
            },
        ];
        render(
            <WizardNavigation
                selectedCategory={0}
                inputData={dummyData}
                navsObj={{ 'Nav #1': {}, 'Nav #2': {} }}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                validateInput={validateInput}
                assertAuthorization={jest.fn()}
            />
        );
        await userEvent.click(screen.getByRole('tab', { name: 'Nav #1' }));
        await userEvent.click(screen.getByRole('tab', { name: 'Nav #2' }));
        await userEvent.click(screen.getByRole('tab', { name: 'YAML result' }));
        expect(validateInput).toHaveBeenCalledTimes(5);
    });
    it('should not validate upon accessing something else', () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const validateInput = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardNavigation
                selectedCategory={3}
                inputData={[]}
                navsObj={{ Nav1: {}, Nav2: {} }}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                validateInput={validateInput}
                assertAuthorization={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.handleChange(1);
        expect(validateInput).toHaveBeenCalledTimes(0);
    });
    it('should ignore certain events', () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardNavigation
                selectedCategory={0}
                inputData={[]}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
            />
        );
        const instance = wrapper.instance();
        instance.handleChange('go');
        expect(changeWizardCategory).toHaveBeenCalledTimes(0);
    });
    it('should load the tabs', () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const checkFilledInput = jest.fn();
        const validateInput = jest.fn();
        const dummyData = [
            {
                text: 'Some Enabler',
                nav: 'Nav #1',
                categories: [
                    { name: 'Category 1', indentation: false },
                    { name: 'Category 2', indentation: false },
                ],
                help: 'Some additional information',
                helpUrl: {
                    title: 'Help',
                    link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
                },
            },
            {
                text: 'Other Enabler',
                nav: 'Nav #1',
                categories: [
                    { name: 'Category 1', indentation: false },
                    { name: 'Category 2', indentation: false },
                ],
            },
        ];
        render(
            <WizardNavigation
                selectedCategory={0}
                inputData={dummyData}
                navsObj={{ 'Nav #1': {} }}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                checkFilledInput={checkFilledInput}
                validateInput={validateInput}
            />
        );
        userEvent.click(screen.getByRole('tab', { name: 'Nav #1' }));
        expect(screen.getByRole('link')).toBeInTheDocument();
        expect(screen.getByRole('tablist')).toBeInTheDocument();
    });

    it('should add a class name for the problematic tabs', () => {
        const next = jest.fn();
        const changeWizardCategory = jest.fn();
        const checkFilledInput = jest.fn();
        const dummyData = [
            {
                text: 'Category 1',
                content: [
                    {
                        test: { value: 'val', question: 'Why?' },
                    },
                ],
                help: 'Some additional information',
                nav: 'Nav #1',
            },
        ];
        render(
            <WizardNavigation
                selectedCategory={0}
                inputData={dummyData}
                navsObj={{ 'Nav #1': { warn: true } }}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                checkFilledInput={checkFilledInput}
            />
        );
        expect(screen.getByLabelText('problem')).toBeInTheDocument();
    });
});
