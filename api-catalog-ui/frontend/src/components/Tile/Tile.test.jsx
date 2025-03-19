/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {fireEvent, screen} from '@testing-library/react';
import {render} from '@testing-library/react'
import Tile from './Tile';
import '@testing-library/jest-dom';

const match = {
    params: {
        serviceID: 'apicatalog',
    },
};

const sampleTile = {
    version: '1.0.0',
    id: 'apicatalog',
    title: 'API Mediation Layer API',
    status: 'UP',
    description: 'lkajsdlkjaldskj',
    services: [
        {
            serviceId: 'apicatalog',
            title: 'API Catalog',
            description:
                'API ML Microservice to locate and display API documentation for API ML discovered microservices',
            status: 'UP',
            secured: false,
            homePageUrl: '/ui/v1/apicatalog',
            sso: true,
        },
        {
            serviceId: 'gateway',
            title: 'API Gateway',
            description:
                'API Gateway to route and authenticate requests to the registered services ',
            status: 'DOWN',
            secured: false,
            homePageUrl: 'gateway/api/v1',
            sso: false,
        },
    ],
    totalServices: 1,
    activeServices: 1,
    lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
    createdTimestamp: '2018-08-22T08:31:22.948+0000',
    sso: true,
};

const mockNavigate = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
    };
});
describe('>>> Tile component tests', () => {

    it('should display status ', () => {
        const {container} = render(<Tile tile={sampleTile} service={sampleTile.services[0]}/>);
        screen.getByTestId('success-icon')
        screen.getByText('The service is running');
    });

    it('method getTileStatusText() should return correct values', () => {
        render(<Tile tile={sampleTile} service={sampleTile.services[1]}/>);
        screen.debug();
        screen.getByTestId('danger-icon')
        screen.getByText('The service is not running');
    });

    it('should handle tile click', () => {
        render(
            <Tile
                fetchNewService={jest.fn()}
                service={sampleTile.services[0]}
            />
        );
        fireEvent.click(screen.getByTestId('tile'))
        expect(mockNavigate).toHaveBeenCalled();
    });

    it('should show sso if it is set', () => {
        render(<Tile tile={sampleTile} service={sampleTile.services[0]}/>);
        screen.getByText('(SSO)');
    });
});
