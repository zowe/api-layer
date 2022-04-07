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
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom/extend-expect';
import WizardDialog from './WizardDialog';
import { categoryData } from './configs/wizard_categories';
import WizardContainer from './WizardContainer';

jest.mock('./WizardComponents/WizardNavigationContainer', () => () => {
    const WizardNavigationContainer = 'WizardNavigationContainerMock';
    return <WizardNavigationContainer />;
});
describe('>>> WizardDialog tests', () => {
    it('should render the dialog if store value is true', () => {
        render(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={categoryData}
                navsObj={{ 'Tab 1': {} }}
                wizardIsOpen
            />
        );
        expect(screen.getByText('Onboard a New API Using')).toBeInTheDocument();
    });
    it('should create 0 inputs if content is an empty object', () => {
        const dummyData = [{ text: 'Basic info', content: [] }];
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={dummyData}
                navsObj={{ 'Basic info': {} }}
                wizardIsOpen
            />
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
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={dummyData}
                navsObj={{ 'Basic info': {} }}
                wizardIsOpen
            />
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
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={dummyData}
                navsObj={{ 'Basic info': {} }}
                wizardIsOpen
            />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });
    it('should close dialog on cancel', () => {
        const wizardToggleDisplay = jest.fn();
        const dummyData = [
            {
                text: 'Basic info',
                content: null,
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={dummyData}
                navsObj={{ 'Basic info': {} }}
            />
        );
        const instance = wrapper.instance();
        instance.closeWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
    });
    it('should check all input on accessing the YAML tab', () => {
        const validateInput = jest.fn();
        const nextWizardCategory = jest.fn();
        const dummyData = [
            { text: 'Basic info', content: null },
            { text: 'IP info', content: null },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={dummyData}
                selectedCategory={dummyData.length - 1}
                navsObj={{ 'Basic info': {}, 'IP info': {} }}
                validateInput={validateInput}
                nextWizardCategory={nextWizardCategory}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(nextWizardCategory).toHaveBeenCalled();
        expect(validateInput).toHaveBeenCalledTimes(3);
        expect(wrapper.find({ size: 'medium' })).toExist();
    });
    it('should not validate all input when not accessing the YAML tab', () => {
        const validateInput = jest.fn();
        const nextWizardCategory = jest.fn();
        const dummyData = [
            { text: 'Basic info', content: null },
            { text: 'IP info', content: null },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={dummyData}
                selectedCategory={0}
                navsObj={{ 'Basic info': {}, 'IP info': {} }}
                validateInput={validateInput}
                nextWizardCategory={nextWizardCategory}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(nextWizardCategory).toHaveBeenCalled();
        expect(validateInput).toHaveBeenCalledTimes(1);
    });
    it('should refresh API and close wizard on Done', () => {
        const wizardToggleDisplay = jest.fn();
        const refreshedStaticApi = jest.fn();
        const createYamlObject = jest.fn();
        const dummyData = [{ text: 'Basic info', content: null }];
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                refreshedStaticApi={refreshedStaticApi}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={dummyData}
                selectedCategory={1}
                navsObj={{ 'Basic info': {} }}
                nextWizardCategory={jest.fn()}
                sendYAML={jest.fn()}
                createYamlObject={createYamlObject}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(wrapper.find({ size: 'large' })).toExist();
    });
    it('should invoke nextCategory on clicking "Next"', () => {
        const nextWizardCategory = jest.fn();
        const validateInput = jest.fn();
        const dummyData = [{ text: 'Basic info', content: null }];
        const wrapper = enzyme.shallow(
            <WizardDialog
                inputData={dummyData}
                selectedCategory={0}
                navsObj={{ 'Basic info': {} }}
                nextWizardCategory={nextWizardCategory}
                validateInput={validateInput}
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(nextWizardCategory).toHaveBeenCalled();
        expect(validateInput).toHaveBeenCalled();
    });
    it('should check presence on clicking "Next"', () => {
        const sendYAML = jest.fn();
        const dummyData = [
            {
                text: 'Basic info',
                content: null,
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog
                enablerName="Static Onboarding"
                inputData={dummyData}
                selectedCategory={1}
                navsObj={{ Basics: { 'Basic info': [[]] } }}
                sendYAML={sendYAML}
                userCanAutoOnboard
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(sendYAML).toHaveBeenCalled();
    });
    it('should throw err on clicking "Next" if presence is insufficient', () => {
        const notifyError = jest.fn();
        const dummyData = [
            {
                text: 'Basic info',
                content: [{ key: { value: '' } }],
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog
                enablerName="Static Onboarding"
                inputData={dummyData}
                selectedCategory={1}
                navsObj={{ Basics: { 'Basic info': [['key']], silent: true } }}
                notifyError={notifyError}
                userCanAutoOnboard
            />
        );
        const instance = wrapper.instance();
        instance.nextSave();
        expect(notifyError).toHaveBeenCalled();
    });
    it('should render yaml file upload button and labels', () => {
        render(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={categoryData}
                navsObj={{ 'Tab 1': {} }}
                wizardIsOpen
            />
        );
        expect(screen.getByText('Select your YAML configuration file to prefill the fields:')).toBeInTheDocument();
        expect(screen.getByText('Choose File')).toBeInTheDocument();
        expect(screen.getByText('Or fill the fields:')).toBeInTheDocument();
    });
    it('should upload a yaml file and fill the corresponding inputs', async () => {
        
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                inputData={categoryData}
                navsObj={{ 'Tab 1': {} }}
                wizardIsOpen
                updateUploadedYamlTitle={jest.fn()}
            />
        );

        // Setup spies
        const readAsText = jest.spyOn(FileReader.prototype, 'readAsText');
        const instance = wrapper.instance();
        const fillInputs = jest.spyOn(instance, "fillInputs");
        const updateYamlTitle = jest.spyOn(instance.props, "updateUploadedYamlTitle");

        const fileContents = `serviceId: enablerjavasampleapp
title: Onboarding Enabler Java Sample App`;
        const expectedFileConversion = { "serviceId": "enablerjavasampleapp", "title": "Onboarding Enabler Java Sample App" }

        const filename = 'testEnabler1.yaml';
        const fakeFile = new File([fileContents], filename);
        
        const input = wrapper.find('#yaml-browser');
        input.simulate('change', { target: { value: 'C:\\fakepath\\' + filename, files: [fakeFile]  }, preventDefault: jest.fn() });
        
        // Must wait slightly for the file to actually be read in by the system (triggers the reader.onload event)
        const pauseFor = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
        await pauseFor(300);

        expect(readAsText).toBeCalledWith(fakeFile);
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(expectedFileConversion);
        expect(updateYamlTitle).toHaveBeenCalledTimes(1);
        expect(updateYamlTitle).toBeCalledWith(filename);
        // expect(fillInputs).toBeCalledWith("ogogogo");

        // expect(wrapper.find('#yaml-file-text')).toExist();


        // const { getByTestId, queryByTestId } = render(
        //     <WizardDialog
        //         wizardToggleDisplay={jest.fn()}
        //         inputData={categoryData}
        //         navsObj={{ 'Tab 1': {} }}
        //         wizardIsOpen
        //     />
        // );

        // const fileContents = require('./../../../cypress/fixtures/enabler-test-files/testEnabler1.yaml');
        // const fakeFile = new File([fileContents], 'testEnabler1.yaml');

        // const input = getByTestId(/yaml-upload-test/i);

        
        // // fireEvent.change(input, {
        // //     target: { value: 'C:\\fakepath\\testEnabler1.yaml', files: [fakeFile] },
        // // })
        // userEvent.upload(input, fakeFile);
        // const pauseFor = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
        // await pauseFor(2000);
        // //await userEvent.upload(input, fakeFile);
        // expect(getByTestId(/yaml-upload-test/i)).toHaveValue('testEnabler1.yaml')
        // //expect(queryByTestId(/yaml-upload-test/i)).toBeTruthy();
        
    });
});
