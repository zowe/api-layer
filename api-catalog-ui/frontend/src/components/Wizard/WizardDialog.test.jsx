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
import '@testing-library/jest-dom/extend-expect';
import WizardDialog from './WizardDialog';
import { categoryData } from './configs/wizard_categories';
import { inputDataCombinedEnabler } from './WizardDialogTestData';

beforeEach(() => {
    jest.clearAllMocks();
});

afterEach(() => {
    jest.clearAllMocks();
});

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
        const convertedCategoryData = Object.keys(categoryData);
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={convertedCategoryData}
                navsObj={{ 'Tab 1': {} }}
                wizardIsOpen
                updateUploadedYamlTitle={jest.fn()}
                notifyInvalidYamlUpload={jest.fn()}
                validateInput={jest.fn()}
            />
        );

        // Setup spies
        const readAsText = jest.spyOn(FileReader.prototype, 'readAsText');
        const instance = wrapper.instance();
        const fillInputs = jest.spyOn(instance, 'fillInputs');
        const updateYamlTitle = jest.spyOn(instance.props, 'updateUploadedYamlTitle');

        // Setup the file to be uploaded
        const fileContents = `serviceId: enablerjavasampleapp
title: Onboarding Enabler Java Sample App`;
        const expectedFileConversion = {
            serviceId: 'enablerjavasampleapp',
            title: 'Onboarding Enabler Java Sample App',
        };

        const filename = 'brokenFile.yaml';
        const fakeFile = new File([fileContents], filename);

        // Simulate the onchange event
        const input = wrapper.find('#yaml-browser');
        input.simulate('change', {
            target: { value: `C:\\fakepath\\${filename}`, files: [fakeFile] },
            preventDefault: jest.fn(),
        });

        // Must wait slightly for the file to actually be read in by the system (triggers the reader.onload event)
        const pauseFor = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
        await pauseFor(300);

        // Check that all functions are called as expected
        expect(readAsText).toBeCalledWith(fakeFile);
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(expectedFileConversion);
        expect(updateYamlTitle).toHaveBeenCalledTimes(1);
        expect(updateYamlTitle).toBeCalledWith(filename);
    });
    it('should call fillInputs for the enablers', async () => {
        const testNavsObj = {
            Basics: true,
            URL: true,
            Routes: true,
            Authentication: true,
            'API Info': true,
            'Catalog UI Tiles': true,
        };
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={inputDataCombinedEnabler}
                wizardIsOpen
                updateUploadedYamlTitle={jest.fn()}
                notifyInvalidYamlUpload={jest.fn()}
                validateInput={jest.fn()}
                navsObj={testNavsObj}
            />
        );

        // Setup spies
        const instance = wrapper.instance();
        const fillInputs = jest.spyOn(instance, 'fillInputs');
        const validateInput = jest.spyOn(instance.props, 'validateInput');

        // Set up data to call fillInputs
        const testData = {
            services: [
                {
                    serviceId: 'sampleservice',
                    title: 'Hello API ML',
                    description:
                        'Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem',
                    instanceBaseUrls: ['http://localhost:8080'],
                    homePageRelativeUrl: '/home',
                    statusPageRelativeUrl: '/application/info',
                    healthCheckRelativeUrl: '/application/health',
                    routes: [
                        {
                            gatewayUrl: '/api/v1',
                            serviceRelativeUrl: '/sampleservice/api/v1',
                        },
                        {
                            gatewayUrl: '/api/v2',
                            serviceRelativeUrl: '/sampleservice/api/v2',
                        },
                    ],
                    authentication: {
                        scheme: 'bypass',
                        applid: '',
                        headers: 'X-Certificate-Public',
                    },
                    apiInfo: [
                        {
                            apiId: 'test.test.test',
                            version: '1.1.1',
                            gatewayUrl: '/api/v1',
                        },
                    ],
                },
            ],
            catalogUiTiles: {
                apicatalog: {
                    title: 'API Mediation Layer API',
                    description:
                        'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                },
            },
            catalog: [
                {
                    tile: {
                        id: 'apicatalog',
                        title: 'API Mediation Layer API',
                        description:
                            'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                        version: '1.0.0',
                    },
                },
            ],
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(6);
    });
});
