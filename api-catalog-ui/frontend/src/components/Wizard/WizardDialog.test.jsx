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
import { baseCategories } from './configs/wizard_base_categories';
import { springSpecificCategories } from './configs/wizard_spring_categories';
import { staticSpecificCategories } from './configs/wizard_static_categories';
import { nodeSpecificCategories } from './configs/wizard_node_categories';
import { micronautSpecificCategories } from './configs/wizard_micronaut_categories';

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
    // it('should upload a file and fail because it is not yaml format', async () => {
    //     const convertedCategoryData = Object.keys(categoryData);
    //     const wrapper = enzyme.shallow(
    //         <WizardDialog
    //             wizardToggleDisplay={jest.fn()}
    //             updateWizardData={jest.fn()}
    //             inputData={convertedCategoryData}
    //             navsObj={{ 'Tab 1': {} }}
    //             wizardIsOpen
    //             updateUploadedYamlTitle={jest.fn()}
    //             notifyInvalidYamlUpload={jest.fn()}
    //             validateInput={jest.fn()}
    //         />
    //     );

    //     // Setup spies
    //     const readAsText = jest.spyOn(FileReader.prototype, 'readAsText');
    //     const instance = wrapper.instance();
    //     const fillInputs = jest.spyOn(instance, "fillInputs");
    //     const updateYamlTitle = jest.spyOn(instance.props, "updateUploadedYamlTitle");
    //     const notifyInvalidYamlUpload = jest.spyOn(instance.props, "notifyInvalidYamlUpload")

    //     // Setup the file to be uploaded
    //     const fileContents = `give this file some invalid yaml
    //     this is not valid
    //     i am not valid`;

    //     const filename = 'testEnabler1.yaml';
    //     const fakeFile = new File([fileContents], filename);

    //     // Simulate the onchange event
    //     const input = wrapper.find('#yaml-browser');
    //     input.simulate('change', { target: { value: 'C:\\fakepath\\' + filename, files: [fakeFile]  }, preventDefault: jest.fn() });

    //     // Must wait slightly for the file to actually be read in by the system (triggers the reader.onload event)
    //     const pauseFor = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
    //     await pauseFor(300);

    //     // Check that all functions are called as expected
    //     expect(readAsText).toBeCalledWith(fakeFile);
    //     expect(fillInputs).toHaveBeenCalledTimes(0);
    //     expect(updateYamlTitle).toHaveBeenCalledTimes(0);
    //     expect(notifyInvalidYamlUpload).toHaveBeenCalledTimes(1);
    // });
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
    it('should call fillInputs for the Plain Java Enabler', async () => {
        const testNavsObj = {
            Basics: true,
            'IP info': true,
            URL: true,
            'Discovery Service URL': true,
            Routes: true,
            Authentication: true,
            'API Info': true,
            Catalog: true,
            SSL: true,
        };
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={baseCategories}
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
            serviceId: 'enablerjavasampleapp',
            title: 'Onboarding Enabler Java Sample App',
            description: 'Example for exposing a Jersey REST API using Onboarding Enabler Java',
            baseUrl: 'https://localhost:10016/enablerJavaSampleApp',
            serviceIpAddress: '127.0.0.1',
            preferIpAddress: true,
            homePageRelativeUrl: null,
            statusPageRelativeUrl: '/application/info',
            healthCheckRelativeUrl: '/application/health',
            discoveryServiceUrls: ['https://localhost:10011/eureka', 'https://localhost:10012/eureka'],
            routes: [
                {
                    gatewayUrl: 'api/v1',
                    serviceUrl: '/enablerJavaSampleApp/api/v1',
                },
                {
                    gatewayUrl: 'ui/v1',
                    serviceUrl: '/',
                },
            ],
            apiInfo: [
                {
                    apiId: 'zowe.apiml.enabler.java.sample',
                    version: '1.1.1',
                    gatewayUrl: 'api/v1',
                    swaggerUrl: 'https://localhost:10016/enablerJavaSampleApp/openapi.json',
                },
            ],
            catalog: {
                tile: {
                    id: 'cademoapps',
                    title: 'Sample API Mediation Layer Applications',
                    description:
                        'Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem',
                    version: '1.0.0',
                },
            },
            ssl: {
                verifySslCertificatesOfServices: true,
                protocol: 'TLSv1.2',
                keyAlias: 'localhost',
                keyPassword: 'password',
                keyStore: '../keystore/localhost/localhost.keystore.p12',
                keyStorePassword: 'password',
                keyStoreType: 'PKCS12',
                trustStore: '../keystore/localhost/localhost.truststore.p12',
                trustStorePassword: 'password',
                trustStoreType: 'PKCS12',
            },
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(9);
    });
    it('should call fillInputs for the Spring Enabler', async () => {
        const testNavsObj = {
            Basics: true,
            'Scheme info': true,
            'IP & URL': true,
            'Discovery Service URL': true,
            Routes: true,
            'API Info': true,
            Catalog: true,
            'Auth & SSL': true,
        };
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={springSpecificCategories}
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
            enabled: true,
            enableUrlEncodedCharacters: false,
            service: {
                serviceId: 'test',
                title: 'Test',
                description: 'A test',
                scheme: 'https',
                hostname: 'testhost',
                port: '12345',
                contextPath: '/${apiml.service.serviceId}',
                baseUrl: 'https://testhost:12345',
                homePageRelativeUrl: '${apiml.service.contextPath}/',
                statusPageRelativeUrl: '${apiml.service.contextPath}/',
                healthCheckRelativeUrl: '${apiml.service.contextPath}/',
                discoveryServiceUrls: ['http://testhost:12345'],
                routes: [
                    {
                        gatewayUrl: '/api/v1',
                        serviceUrl: '/enablerJavaSampleApp/api/v1',
                    },
                ],
                apiInfo: [
                    {
                        apiId: 'test',
                        version: '1.0.0',
                        gatewayUrl: '/api/v1',
                        swaggerUrl:
                            '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}${apiml.service.contextPath}',
                    },
                ],
                catalog: {
                    tile: {
                        id: 'apicatalog',
                        title: 'API Mediation Layer API',
                        description:
                            'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
                        version: '1.0.0',
                    },
                },
                authentication: {
                    scheme: 'bypass',
                },
                ssl: {
                    verifySslCertificatesOfServices: true,
                    protocol: 'TL',
                    keyAlias: 'test',
                    keyPassword: 'test',
                    keyStore: 'test',
                    keyStorePassword: 'test',
                    keyStoreType: 'PKCS12',
                    trustStore: 'test',
                    trustStorePassword: 'test',
                    trustStoreType: 'PKCS12',
                },
            },
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(8);
    });
    it('should call fillInputs for the Micronaut Enabler', async () => {
        const testNavsObj = {
            Basics: true,
            'Scheme Info': true,
            URL: true,
            Routes: true,
            'API info': true,
            'Catalog configuration': true,
            SSL: true,
            'Micronaut configuration': true,
            'Micronaut SSL configuration': true,
            'Micronaut management': true,
        };
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={staticSpecificCategories}
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
            apiml: {
                service: {
                    preferIpAddress: true,
                    serviceId: 'test',
                    title: 'Test App',
                    description: 'A test app for testing',
                    discoveryServiceUrls: 'http://localhost:12345',
                    scheme: 'https',
                    hostname: 'test',
                    port: '123',
                    contextPath: '/${apiml.service.serviceId}',
                    baseUrl: '${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}',
                    homePageRelativeUrl: '${apiml.service.contextPath}',
                    statusPageRelativeUrl: '${apiml.service.contextPath}',
                    healthCheckRelativeUrl: '${apiml.service.contextPath}',
                    routes: [
                        {
                            gatewayUrl: '/api/v1',
                            serviceUrl: '/service/api/v1',
                        },
                    ],
                    apiInfo: [
                        {
                            apiId: 'my.app.for.testing',
                            version: '1.0.0',
                            gatewayUrl: '${apiml.service.routes.gatewayUrl}',
                        },
                    ],
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
                    ssl: [
                        {
                            enabled: false,
                        },
                    ],
                },
            },
            micronaut: {
                application: {
                    name: '${apiml.service.serviceId}',
                },
                server: {
                    port: '${apiml.service.port}',
                    'context-path': '/${apiml.service.serviceId}',
                },
                ssl: {
                    enable: false,
                    'key-store': {
                        password: '${apiml.service.ssl[0].keyPassword}',
                        type: '${apiml.service.ssl[0].keyStoreType}',
                        path: 'file:${apiml.service.ssl[0].keyStore}',
                    },
                    key: {
                        alias: '${apiml.service.ssl[0].keyAlias}',
                        password: '${apiml.service.ssl[0].keyPassword}',
                    },
                    'trust-store': {
                        password: '${apiml.service.ssl[0].trustStorePassword}',
                        path: 'file:${apiml.service.ssl[0].trustStore}',
                        type: '${apiml.service.ssl[0].trustStoreType}',
                    },
                    port: '${apiml.service.port}',
                    ciphers: '${apiml.service.ssl[0].ciphers}',
                    protocol: '${apiml.service.ssl[0].protocol}',
                },
            },
            eureka: {
                client: {
                    serviceUrl: {
                        defaultZone: 'https://localhost:44212',
                    },
                },
            },
            management: {
                endpoints: {
                    web: {
                        'base-path': '/thing',
                        exposure: {
                            include: 'yep,yepp',
                        },
                    },
                    health: {
                        defaults: {
                            enabled: false,
                        },
                    },
                },
            },
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(10);
    });
    it('should call fillInputs for the Node JS Enabler', async () => {
        const testNavsObj = {
            Basics: true,
            Instance: true,
            'Instance ports': true,
            'Instance info': true,
            'Instance metadata': true,
            SSL: true,
        };
        const wrapper = enzyme.shallow(
            <WizardDialog
                wizardToggleDisplay={jest.fn()}
                updateWizardData={jest.fn()}
                inputData={nodeSpecificCategories}
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
            eureka: {
                ssl: false,
                host: 'localhost',
                ipAddress: '127.0.0.1',
                port: '10011',
                servicePath: '/eureka/apps/',
                maxRetries: 30,
                requestRetryDelay: 1000,
                registryFetchInterval: 5,
            },
            instance: {
                app: '${serviceId}',
                vipAddress: '${serviceId}',
                instanceId: 'localhost:hwexpress:10020',
                homePageUrl: '${homePageRelativeUrl}',
                hostname: 'localhost',
                ipAddr: '127.0.0.1',
                secureVipAddress: '${serviceId}',
                port: {
                    $: '10020',
                    '@enabled': false,
                },
                securePort: {
                    $: '10020',
                    '@enabled': true,
                },
                dataCenterInfo: {
                    '@class': 'com.test',
                    name: 'Test',
                },
                metadata: {
                    'apiml.catalog.tile.id': 'samplenodeservice',
                    'apiml.catalog.tile.title': 'Zowe TEST',
                    'apiml.catalog.tile.description': 'test',
                    'apiml.catalog.tile.version': '1.1.1',
                    'apiml.routes.api_v1.gatewayUrl': '${routes.gatewayUrl}',
                    'apiml.routes.api_v1.serviceUrl': '${routes.serviceUrl}',
                    'apiml.apiInfo.0.apiId': 'test.test',
                    'apiml.apiInfo.0.gatewayUrl': '${routes.gatewayUrl}',
                    'apiml.apiInfo.0.swaggerUrl': 'http://localhost:10020/swagger.json',
                    'apiml.service.title': 'Test Service',
                    'apiml.service.description': 'a service for testing',
                },
            },
            ssl: {
                certificate: 'ssl/localhost.keystore.cer',
                keyStore: 'ssl/localhost.keystore.cer',
                caFile: 'ssl/localhost.kpen',
                keyPassword: '123',
            },
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(6);
    });
    it('should call fillInputs for the Static Onboarding', async () => {
        const convertedCategoryData = Object.keys(categoryData);
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
                inputData={micronautSpecificCategories}
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
                    instanceBaseUrls: ['http://localhost:8080',],
                    homePageRelativeUrl: '/home',
                    statusPageRelativeUrl: '/application/info',
                    healthCheckRelativeUrl: '/application/health',
                    routes: [
                        {
                            gatewayUrl: '/api/v1',
                            serviceRelativeUrl: '/sampleservice/api/v1',
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
        };

        // Call the function to test it
        instance.fillInputs(testData);

        // Check that all functions are called as expected
        expect(fillInputs).toHaveBeenCalledTimes(1);
        expect(fillInputs).toHaveBeenCalledWith(testData);
        expect(validateInput).toHaveBeenCalledTimes(6);
    });
});
