/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {screen} from '@testing-library/react';
import ServiceTab from './ServiceTab';
import {renderWithProviders} from "../../helpers/test-utils";
import '@testing-library/jest-dom';
import userEvent from "@testing-library/user-event";
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';
import GraphQLUIApiml from '../GraphQL/GraphQLUIApiml'

const service = {
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
    apis: {
        'org.zowe v1': {gatewayUrl: 'api/v1'},
        'org.zowe v2': {gatewayUrl: 'api/v2'}
    },
    instances: ["localhost:gateway:10010"]
};

const serviceDown = {
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
    apis: {'org.zowe v1': {gatewayUrl: 'api/v1'}},
};
const serviceGraphql = {
    serviceId: 'graphqlservice',
    title: 'Graphql API',
    description:
        'Graphql API demo.',
    status: 'UP',
    baseUrl: 'https://localhost:6000',
    homePageUrl: 'https://localhost:10010/',
    basePath: '/gateway/api/v1',
    apiDoc: null,
    apiVersions: ['org.zowe v1', 'org.zowe v2'],
    defaultApiVersion: ['org.zowe v1'],
    ssoAllInstances: true,
    apis: {'org.zowe v1': {graphqlUrl: 'api/v1'}},
};

const mockNavigate = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
    };
});

jest.mock("../Swagger/SwaggerContainer", () => ({
    __esModule: true,
    default: jest.fn(() => ({})),
}));

jest.mock("../ServiceVersionDiff/ServiceVersionDiffContainer", () => ({
    __esModule: true,
    default: jest.fn(() => ({})),
}));
jest.mock("../GraphQL/GraphQLUIApiml", () => ({
    __esModule: true,
    default: jest.fn(() => ({})),
}));
describe('>>> ServiceTab component tests', () => {
    it('should display service tab information', () => {

        renderWithProviders(
            <ServiceTab
                service={service}
            />
        );
        const link = screen.getByTestId('link')

        expect(link).toHaveAttribute('href', 'https://localhost:10010/')
        screen.getByText('API Gateway');
        screen.getByTestId('sso')
        screen.getByTestId('service-id')
        screen.getByTestId('base-path')
    });

    it('should change selected version when clicking v2 api version', async () => {
        renderWithProviders(
            <ServiceTab
                service={service}
            />
        );
        await userEvent.click(screen.getByText('org.zowe v1'))
        await userEvent.click(screen.getByText('org.zowe v2'))
        screen.getByText('org.zowe v2');

    });


    it('should display information that service is not running if service down', () => {
        renderWithProviders(
            <ServiceTab
                service={serviceDown}
            />
        );
        screen.getByTitle('API Homepage navigation is disabled as the service is not running')
    });

    it('should display graphql when service provides graphql url', () => {
        renderWithProviders(
            <ServiceTab
                service={serviceGraphql}
            />
        );
        expect(GraphQLUIApiml).toHaveBeenCalled();
    });


    it('should call handleDialogOpen on button click', async () => {
        renderWithProviders(
            <ServiceTab
                service={service}
            />
        );
        await userEvent.click(screen.getByTestId('diff-button'));
        expect(ServiceVersionDiffContainer).toHaveBeenCalled()
    });

});
