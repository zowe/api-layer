/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import React from 'react';
import WizardDialog from './WizardDialog';
import { categoryData } from './configs/wizard_categories';
xdescribe('>>> WizardDialog tests', () => {
    it('should render the dialog if store value is true', () => {
        const wrapper = enzyme.shallow(<WizardDialog wizardToggleDisplay={jest.fn()} inputData={categoryData} wizardIsOpen />);
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
    });
    it('should create 0 inputs if content is an empty object', () => {
        const dummyData = [
            {
                text: 'Basic info',
                content: {},
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });
    it('should create 0 inputs if content does not exist', () => {
        const dummyData = [
            {
                text: 'Basic info',
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });
    it('should create 0 inputs if content is null', () => {
        const dummyData = [
            {
                text: 'Basic info',
                content: null,
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });
    it('should close dialog on cancel', () => {
        const wizardToggleDisplay = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={categoryData}
            />
        );
        const instance = wrapper.instance();
        instance.closeWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
    });
    it('should close dialog and refresh static APIs on Save', () => {
        const wizardToggleDisplay = jest.fn();
        const refreshedStaticApi = jest.fn();
        const createYamlObject = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                refreshedStaticApi={refreshedStaticApi}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={categoryData}
                selectedCategory={categoryData.length}
                nextWizardCategory={jest.fn()}
                createYamlObject={createYamlObject}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(wizardToggleDisplay).toHaveBeenCalled();
        expect(refreshedStaticApi).toHaveBeenCalled();
        expect(createYamlObject).toHaveBeenCalled();
    });
    it('should invoke nextCategory on clicking "Next"', () => {
        const nextWizardCategory = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={jest.fn()}
                refreshedStaticApi={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={categoryData}
                selectedCategory={0}
                nextWizardCategory={nextWizardCategory}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(nextWizardCategory).toHaveBeenCalled();
    });
});
