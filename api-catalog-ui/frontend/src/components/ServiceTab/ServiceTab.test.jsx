/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { shallow } from 'enzyme';
import ServiceTab from './ServiceTab';

const params = {
    path: '/service/:serviceID/:serviceId',
    url: '/tile/apimediationlayer/gateway',
    params: {
        serviceId: 'gateway',
    },
};
const selectedService = {
    serviceId: 'gateway',
    title: 'API Gateway',
    description:
        'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.',
    status: 'UP',
    baseUrl: 'https://localhost:6000',
    homePageUrl: 'https://localhost:10010/',
    basePath: '/gateway/api/v1',
    apiDoc: null,
    apiVersions: ['org.zowe v1', 'org.zowe v2'],
    defaultApiVersion: ['org.zowe v1'],
    ssoAllInstances: true,
    apis: { 'org.zowe v1': { gatewayUrl: 'api/v1' } },
};

const selectedServiceDown = {
    serviceId: 'gateway',
    title: 'API Gateway',
    description:
        'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.',
    status: 'DOWN',
    baseUrl: 'https://localhost:6000',
    homePageUrl: 'https://localhost:10010/',
    basePath: '/gateway/api/v1',
    apiDoc: null,
    apiVersions: ['org.zowe v1', 'org.zowe v2'],
    defaultApiVersion: ['org.zowe v1'],
    ssoAllInstances: true,
    apis: { 'org.zowe v1': { gatewayUrl: 'api/v1' } },
};

const tiles = {
    version: '1.0.0',
    id: 'apimediationlayer',
    title: 'API Mediation Layer API',
    status: 'UP',
    description:
        'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
    services: [selectedService],
};
describe('>>> ServiceTab component tests', () => {
    beforeEach(() => {
        process.env.REACT_APP_API_PORTAL = false;
    });
    it('should display service tab information', () => {
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        serviceTab.setState({ selectedVersion: 'org.zowe v1' });

        expect(serviceTab.find('[data-testid="tooltip"]').exists()).toEqual(true);
        expect(serviceTab.find('[data-testid="link"]').exists()).toEqual(true);
        expect(serviceTab.find('[data-testid="link"]').props().href).toEqual('https://localhost:10010/');
        expect(serviceTab.find('[data-testid="service"]').prop('children')).toEqual('API Gateway');

        const checkValueItem = function (serviceTabElement, selector, title, value) {
            const row = serviceTabElement.find(selector);
            expect(row.find('label').prop('children')).toEqual(title);
            expect(row.find('span').prop('children')).toEqual(value);
        };

        checkValueItem(serviceTab, '[data-testid="base-path"]', 'API Base Path:', '/gateway/api/v1');
        checkValueItem(serviceTab, '[data-testid="service-id"]', 'Service ID:', 'gateway');
        checkValueItem(serviceTab, '[data-testid="sso"]', 'SSO:', 'supported');
        expect(serviceTab.find('[data-testid="description"]').prop('children')).toEqual(
            'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.'
        );
        expect(serviceTab.find('[data-testid="version"]').first().prop('children')).toEqual('org.zowe v1');
        expect(serviceTab.find('[data-testid="version"]').at(1).prop('children')).toEqual('org.zowe v2');
    });

    it('should change selected version when clicking v2 api version', () => {
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        serviceTab.setState({ selectedVersion: 'org.zowe v1' });

        const dropDownMenu = serviceTab.find('[data-testid="version-menu"]').first();
        dropDownMenu.simulate('click');

        expect(dropDownMenu.children().first().text()).toEqual('org.zowe v1');
        expect(dropDownMenu.children().at(1).text()).toEqual('org.zowe v2');
        dropDownMenu.children().at(1).simulate('click');
        expect(serviceTab.find('[data-testid="version-menu"]').first().text()).toBe('org.zowe v1org.zowe v2');
    });

    it('should throw error when tiles are null', () => {
        const selectService = jest.fn();
        expect(() =>
            shallow(
                <ServiceTab
                    match={params}
                    selectedService={selectedService}
                    tiles={null}
                    selectService={selectService}
                />
            )
        ).toThrow('No tile is selected.');
    });

    it('should throw error when tiles are undefined', () => {
        const selectService = jest.fn();
        expect(() =>
            shallow(
                <ServiceTab
                    match={params}
                    selectedService={selectedService}
                    tiles={undefined}
                    selectService={selectService}
                />
            )
        ).toThrow('No tile is selected.');
    });

    it('should throw error when tiles are empty', () => {
        const selectService = jest.fn();
        expect(() =>
            shallow(
                <ServiceTab match={params} selectedService={selectedService} tiles={[]} selectService={selectService} />
            )
        ).toThrow('No tile is selected.');
    });

    it('should display default footer for custom portal in case of additional content', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
                useCasesCounter={1}
                videosCounter={2}
                tutorialsCounter={1}
            />
        );
        expect(serviceTab.find('.footer-labels').exists()).toEqual(true);
        expect(serviceTab.find('#detail-footer').exists()).toEqual(true);
    });

    it('should not display default footer for custom portal in case there is not additional content', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
                useCasesCounter={0}
                videosCounter={0}
                tutorialsCounter={0}
            />
        );
        expect(serviceTab.find('.footer-labels').exists()).toEqual(false);
    });

    it('should display home page link if service down', () => {
        process.env.REACT_APP_API_PORTAL = false;
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedServiceDown}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        expect(serviceTab.find('[data-testid="red-homepage"]').exists()).toEqual(true);
    });

    it('should update state correctly when selectedVersion is null', () => {
        process.env.REACT_APP_API_PORTAL = false;
        const selectService = jest.fn();
        const wrapper = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedServiceDown}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        wrapper.setState({ selectedVersion: null });

        wrapper.instance().handleDialogOpen(selectedService);

        expect(wrapper.state().isDialogOpen).toEqual(true);
        expect(wrapper.state().selectedVersion).toEqual('diff');
        expect(wrapper.state().previousVersion).toEqual(selectedService.defaultApiVersion);
    });

    it('should call handleDialogOpen on button click', () => {
        const selectService = jest.fn();
        const wrapper = shallow(
            <ServiceTab match={params} selectedService={selectService} tiles={[tiles]} selectService={selectService} />
        );
        const handleDialogOpenSpy = jest.spyOn(wrapper.instance(), 'handleDialogOpen');

        const button = wrapper.find('#compare-button');
        button.simulate('click');

        expect(handleDialogOpenSpy).toHaveBeenCalledTimes(1);
    });

    it('should disable the button and dropdown if apiVersions length is less than 2', () => {
        const selectService = jest.fn();
        const apiVersions = ['1.0.0'];
        selectedService.apiVersions = apiVersions;
        const wrapper = shallow(
            <ServiceTab match={params} selectedService={selectService} tiles={[tiles]} selectService={selectService} />
        );
        wrapper.setState({ apiVersions });

        const button = wrapper.find('#compare-button');
        const dropdownMenu = wrapper.find('#version-menu');
        expect(button.prop('disabled')).toEqual(true);
        expect(dropdownMenu.prop('disabled')).toEqual(true);
        expect(button.prop('style')).toEqual({
            backgroundColor: '#e4e4e4',
            color: '#6b6868',
            opacity: '0.5',
        });
        expect(dropdownMenu.prop('style')).toEqual({
            color: '#6b6868',
            opacity: '0.5',
        });
    });

    it('should enable the button if apiVersions length is greater than or equal to 2', () => {
        const selectService = jest.fn();
        const wrapper = shallow(
            <ServiceTab match={params} selectedService={selectService} tiles={[tiles]} selectService={selectService} />
        );
        const apiVersions = ['org.zowe v1', 'org.zowe v2'];
        selectedService.apiVersions = apiVersions;
        wrapper.setState({ apiVersions });

        const button = wrapper.find('#compare-button');

        expect(button.prop('disabled')).toEqual(false);
        expect(button.prop('style')).toEqual({
            backgroundColor: '#fff',
            color: '#0056B3',
        });
    });
});
