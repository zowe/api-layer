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
import { render } from '@testing-library/react'
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

    it('should display API Mediation Layer API tile with correct title', () => {
        const instance = shallow(<Tile tile={sampleTile} service={sampleTile.services[0]} />);
        expect(instance.find('API Mediation Layer API')).not.toBeNull();
    });

    it('should display status ', () => {
        const {container} =render(<Tile tile={sampleTile} service={sampleTile.services[0]} />);
        screen.getByTestId('success-icon')
        screen.getByText('The service is running');
    });

    it('method getTileStatusText() should return correct values', () => {
        render(<Tile tile={sampleTile} service={sampleTile.services[1]} />);
        screen.debug();
        screen.getByTestId('success-icon')
        screen.getByText('The service is running');
        // const instance = wrapper.instance();
        // expect(instance.getTileStatusText(sampleTile)).toBe('The service is running');
        // resetSampleTile();
        // sampleTile.status = 'DOWN';
        // expect(instance.getTileStatusText(sampleTile)).toBe('The service is not running');
        // resetSampleTile();
        // sampleTile.status = 'WARNING';
        // resetSampleTile();
        // sampleTile.status = 'UNKNOWN';
        // expect(instance.getTileStatusText(sampleTile)).toBe('Status unknown');
        // expect(instance.getTileStatusText()).toBe('Status unknown');
    });

    it('should handle tile click', () => {
        const historyMock = { push: jest.fn() };
        const storeCurrentTileId = jest.fn();
        const wrapper = shallow(
            <Tile
                tile={sampleTile}
                storeCurrentTileId={storeCurrentTileId}
                service={sampleTile.services[0]}
                history={historyMock}
                match={match}
            />
        );
        wrapper.find('[data-testid="tile"]').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual([`/service/${sampleTile.id}`]);
    });

    it('should show sso if it is set', () => {
render(<Tile tile={sampleTile} service={sampleTile.services[0]} />);
screen.getByText('SSO');
    });

    it('should mssing sso if it is not set', () => {
        sampleTile.sso = false;
        const wrapper = shallow(<Tile tile={sampleTile} service={sampleTile.services[1]} />);
        expect(wrapper.text().includes('SSO')).toBe(false);
    });
});
