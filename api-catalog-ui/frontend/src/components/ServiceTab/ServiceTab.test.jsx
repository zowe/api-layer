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
    xit('should display service tab information', () => {
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
});
