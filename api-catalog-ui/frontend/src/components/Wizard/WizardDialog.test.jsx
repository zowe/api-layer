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
import '@testing-library/jest-dom';
import WizardDialog from './WizardDialog';
import { categoryData } from './configs/wizard_categories';

beforeEach(() => {
    jest.clearAllMocks();
});

afterEach(() => {
    jest.clearAllMocks();
});

jest.mock(
    './WizardComponents/WizardNavigationContainer',
    () =>
        function () {
            const WizardNavigationContainer = 'WizardNavigationContainerMock';
            return <WizardNavigationContainer />;
        }
);
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
        // eslint-disable-next-line no-promise-executor-return
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
                inputData={[
                    {
                        text: 'Basic info',
                        content: [
                            {
                                serviceId: {
                                    value: 'sampleservice',
                                    question: 'A unique identifier for the API (service ID):',
                                    maxLength: 40,
                                    lowercase: true,
                                    tooltip: 'Example: sampleservice',
                                    show: true,
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                title: {
                                    value: 'Hello API ML',
                                    question: 'The name of the service (human readable):',
                                    tooltip: 'Example: Hello API ML',
                                    show: true,
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        inArr: true,
                        nav: 'Basics',
                    },
                    {
                        text: 'Description',
                        content: [
                            {
                                description: {
                                    value: 'Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem',
                                    question: 'A concise description of the service:',
                                    tooltip: 'Example: Sample API ML REST Service.',
                                    show: true,
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        inArr: true,
                        nav: 'Basics',
                    },
                    {
                        text: 'Catalog info',
                        content: [
                            {
                                type: {
                                    value: 'Custom',
                                    question: 'Choose existing catalog tile or create a new one:',
                                    options: ['Custom'],
                                    hidden: true,
                                    show: true,
                                },
                                catalogUiTileId: {
                                    value: 'apicatalog',
                                    question: 'The id of the catalog tile:',
                                    regexRestriction: [
                                        {
                                            value: '^[a-zA-Z1-9]+$',
                                            tooltip: 'Only alphanumerical values with no whitespaces are accepted',
                                        },
                                    ],
                                    dependencies: {
                                        type: 'Custom',
                                    },
                                    tooltip: 'Example: static',
                                    show: true,
                                },
                            },
                        ],
                        interference: 'staticCatalog',
                        inArr: true,
                        nav: 'Basics',
                    },
                    {
                        text: 'URL for Static',
                        content: [
                            {
                                instanceBaseUrls: {
                                    value: 'https://localhost:8080',
                                    question: 'The base URL of the instance (the consistent part of the web address):',
                                    tooltip: 'Example: https://localhost:8080',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        multiple: true,
                        noKey: true,
                        indentation: 'instanceBaseUrls',
                        inArr: true,
                        nav: 'URL',
                    },
                    {
                        text: 'URL',
                        content: [
                            {
                                homePageRelativeUrl: {
                                    value: '/home',
                                    question: 'The relative path to the home page of the service:',
                                    optional: true,
                                    validUrl: true,
                                    tooltip:
                                        'Normally used for informational purposes for other services to use it as a landing page. Example: /home',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                statusPageRelativeUrl: {
                                    value: '/application/info',
                                    question: 'The relative path to the status page of the service:',
                                    optional: true,
                                    regexRestriction: [
                                        {
                                            value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
                                            tooltip: 'The relative URL has to be valid, example: /application/info',
                                        },
                                    ],
                                    tooltip: 'Example: /application/info',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                healthCheckRelativeUrl: {
                                    value: '/application/health',
                                    question: 'The relative path to the health check endpoint of the service:',
                                    optional: true,
                                    regexRestriction: [
                                        {
                                            value: '^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*',
                                            tooltip: 'The relative URL has to be valid, example: /application/info',
                                        },
                                    ],
                                    tooltip: 'Example: /application/health',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        inArr: true,
                        nav: 'URL',
                    },
                    {
                        text: 'Routes for Static & Node',
                        content: [
                            {
                                gatewayUrl: {
                                    value: '/api/v1',
                                    question: 'Expose the Service API on Gateway under context path:',
                                    tooltip: 'Format: /api/vX, Example: /api/v1',
                                    regexRestriction: [
                                        {
                                            value: '^(/[a-z]+\\/v\\d+)$',
                                            tooltip: 'Format: /api/vX, Example: /api/v1',
                                        },
                                    ],
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                serviceRelativeUrl: {
                                    value: '/sampleservice/api/v1',
                                    question: 'Service API common context path:',
                                    tooltip: 'Example: /sampleservice/api/v1',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        help: 'For service: <service>/allOfMyEndpointsAreHere/** exposed on Gateway under <gateway>/<serviceid>/api/v1/**\nFill in:\ngatewayUrl: /api/v1\nserviceUrl: /allOfMyEndpointsAreHere',
                        multiple: true,
                        indentation: 'routes',
                        inArr: true,
                        minions: {
                            'API Info': ['gatewayUrl'],
                        },
                        nav: 'Routes',
                    },
                    {
                        text: 'Authentication',
                        content: [
                            {
                                scheme: {
                                    value: 'bypass',
                                    question: 'Authentication:',
                                    options: ['bypass', 'zoweJwt', 'httpBasicPassTicket', 'zosmf', 'x509'],
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                applid: {
                                    value: '',
                                    question:
                                        'A service APPLID (valid only for the httpBasicPassTicket authentication scheme ):',
                                    dependencies: {
                                        scheme: 'httpBasicPassTicket',
                                    },
                                    tooltip: 'Example: ZOWEAPPL',
                                    empty: true,
                                },
                                headers: {
                                    value: 'X-Certificate-Public',
                                    question:
                                        'For the x509 scheme use the headers parameter to select which values to send to a service',
                                    dependencies: {
                                        scheme: 'x509',
                                    },
                                    options: [
                                        'X-Certificate-Public',
                                        'X-Certificate-DistinguishedName',
                                        'X-Certificate-CommonName',
                                    ],
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        help: 'The following service authentication schemes are supported by the API Gateway: bypass, zoweJwt, httpBasicPassTicket, zosmf, x509. ',
                        helpUrl: {
                            title: 'More information about the authentication parameters',
                            link: 'https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler/#api-catalog-information',
                        },
                        indentation: 'authentication',
                        inArr: true,
                        nav: 'Authentication',
                    },
                    {
                        text: 'API Info',
                        content: [
                            {
                                apiId: {
                                    value: 'test.test.test',
                                    question: 'A unique identifier to the API in the API ML:',
                                    tooltip: 'Example: zowe.apiml.sampleservice',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                version: {
                                    value: '1.1.1',
                                    question: 'API version:',
                                    tooltip: 'Example: 1.0.0',
                                    regexRestriction: [
                                        {
                                            value: '^(\\d+)\\.(\\d+)\\.(\\d+)$',
                                            tooltip: 'Semantic versioning expected, example: 1.0.7',
                                        },
                                    ],
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                gatewayUrl: {
                                    value: '/api/v1',
                                    question: 'The base path at the API Gateway where the API is available:',
                                    tooltip: 'Format: api/vX, Example: api/v1',
                                    regexRestriction: [
                                        {
                                            value: '^(/[a-z]+\\/v\\d+)$',
                                            tooltip: 'Format: /api/vX, Example: /api/v1',
                                        },
                                    ],
                                    disabled: true,
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                swaggerUrl: {
                                    value: '',
                                    question: 'The Http or Https address where the Swagger JSON document is available:',
                                    optional: true,
                                    tooltip:
                                        'Example: https://${sampleServiceSwaggerHost}:${sampleServiceSwaggerPort}/sampleservice/api-doc',
                                },
                                documentationUrl: {
                                    value: '',
                                    question: 'Link to the external documentation:',
                                    optional: true,
                                    tooltip: 'Example: https://www.zowe.org',
                                },
                            },
                        ],
                        indentation: 'apiInfo',
                        multiple: true,
                        inArr: true,
                        nav: 'API Info',
                        isMinion: true,
                    },
                    {
                        text: 'Catalog UI Tiles',
                        content: [
                            {
                                title: {
                                    value: 'API Mediation Layer API',
                                    question: 'The title of the API services product family:',
                                    tooltip: 'Example: Static API services',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                description: {
                                    value: 'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                                    question: 'The detailed description of the API Catalog UI dashboard tile:',
                                    tooltip:
                                        'Example: Services which demonstrate how to make an API service discoverable in the API ML ecosystem using YAML definitions',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        indentationDependency: 'catalogUiTileId',
                        indentation: 'catalogUiTiles',
                        nav: 'Catalog UI Tiles',
                    },
                    {
                        text: 'Catalog',
                        content: [
                            {
                                type: {
                                    value: 'Custom',
                                    question: 'Choose existing catalog tile or create a new one:',
                                    options: ['Custom'],
                                    hidden: true,
                                },
                                id: {
                                    value: 'apicatalog',
                                    question: 'The unique identifier for the product family of API services:',
                                    tooltip: 'reverse domain name notation. Example: org.zowe.apiml',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                title: {
                                    value: 'API Mediation Layer API',
                                    question: 'The title of the product family of the API service:',
                                    tooltip: 'Example: Hello API ML',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                description: {
                                    value: 'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                                    question: 'A description of the API service product family:',
                                    tooltip:
                                        'Example: Sample application to demonstrate exposing a REST API in the ZOWE API ML',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                                version: {
                                    value: '1.0.0',
                                    question:
                                        'The semantic version of this API Catalog tile (increase when adding changes):',
                                    tooltip: 'Example: 1.0.0',
                                    interactedWith: true,
                                    empty: false,
                                    problem: false,
                                },
                            },
                        ],
                        interference: 'catalog',
                        indentation: 'catalog',
                        multiple: true,
                        arrIndent: 'tile',
                        nav: 'Catalog configuration',
                    },
                    {
                        text: 'Test',
                        content: [],
                        interference: 'test',
                        indentation: 'test/test/test',
                        indentationDependency: 'test',
                        minions: {
                            'Fake Nav': ['nothing'],
                        },
                        nav: 'Test',
                    },
                ]}
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
                    instanceBaseUrls: ['invalidURL', 'http://localhost:8080'],
                    homePageRelativeUrl: 'invalidUrl',
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
            test: false,
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(6);
    });
});
