/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React from 'react';
import * as enzyme from 'enzyme';
import WizardNavigation from './WizardNavigation';
describe('>>> Wizard navigation tests', () => {
    it('should handle category change', () => {
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
        instance.handleChange(2);
        expect(changeWizardCategory).toHaveBeenCalled();
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
        const setNumberOfTabs = jest.fn();
        const dummyData = [
            {
                text: 'Some Enabler',
                nav: 'Nav #1',
                categories: [
                    { name: 'Category 1', indentation: false },
                    { name: 'Category 2', indentation: false },
                ],
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
        const wrapper = enzyme.shallow(
            <WizardNavigation
                selectedCategory={0}
                navTabAmount={0}
                inputData={dummyData}
                nextWizardCategory={next}
                changeWizardCategory={changeWizardCategory}
                setNumberOfTabs={setNumberOfTabs}
            />
        );
        expect(wrapper.find('Tab').length).toEqual(2);
    });
});
