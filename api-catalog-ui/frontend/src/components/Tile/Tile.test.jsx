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
import { render } from 'react-dom';
import { act } from 'react-dom/test-utils';
import Tile from './Tile';

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
        },
    ],
    totalServices: 1,
    activeServices: 1,
    lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
    createdTimestamp: '2018-08-22T08:31:22.948+0000',
    sso: true,
};

const resetSampleTile = () => {
    sampleTile.status = 'UP';
    sampleTile.totalServices = 1;
};

describe('>>> Tile component tests', () => {
    beforeEach(() => {
        resetSampleTile();
    });

    xit('should display API Mediation Layer API tile with correct title', () => {
        const instance = shallow(<Tile tile={sampleTile} />);
        expect(instance.find('API Mediation Layer API')).not.toBeNull();
    });

    xit('method getTileStatus() should return correct values', () => {
        resetSampleTile();
        const wrapper = shallow(<Tile tile={sampleTile} />);
        const instance = wrapper.instance();
        expect(instance.getTileStatus(null).props.id).toBe('unknown');
        expect(instance.getTileStatus(undefined).props.id).toBe('unknown');
        expect(instance.getTileStatus(sampleTile).props.id).toBe('success');
        sampleTile.status = 'DOWN';
        expect(instance.getTileStatus(sampleTile).props.id).toBe('danger');
        sampleTile.totalServices = 2;
        sampleTile.status = 'UP';
        expect(instance.getTileStatus(sampleTile).props.id).toBe('warning');
        sampleTile.status = 'UNKNOWN';
        expect(instance.getTileStatus(sampleTile).props.id).toBe('unknown');
    });

    xit('method getTileStatusText() should return correct values', () => {
        resetSampleTile();
        const wrapper = shallow(<Tile tile={sampleTile} />);
        const instance = wrapper.instance();
        expect(instance.getTileStatusText(sampleTile)).toBe('All services are running');
        sampleTile.totalServices = 2;
        expect(instance.getTileStatusText(sampleTile)).toBe('1 of 2 services are running');
        resetSampleTile();
        sampleTile.status = 'DOWN';
        expect(instance.getTileStatusText(sampleTile)).toBe('No services are running');
        resetSampleTile();
        sampleTile.status = 'WARNING';
        sampleTile.totalServices = 2;
        expect(instance.getTileStatusText(sampleTile)).toBe('1 of 2 services are running');
        resetSampleTile();
        sampleTile.status = 'UNKNOWN';
        expect(instance.getTileStatusText(sampleTile)).toBe('Status unknown');
        expect(instance.getTileStatusText()).toBe('Status unknown');
    });

    xit('should handle tile click', () => {
        const historyMock = { push: jest.fn() };
        const wrapper = shallow(<Tile tile={sampleTile} history={historyMock} match={match} />);
        wrapper.find('[data-testid="tile"]').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual([`/tile/${sampleTile.id}`]);
    });

    xit('should show sso if it is set', () => {
        const container = document.createElement('div');
        act(() => {
            render(<Tile tile={sampleTile} />, container);
        });

        expect(container.textContent).toEqual(expect.stringContaining('SSO'));
    });

    xit('should mssing sso if it is not set', () => {
        sampleTile.sso = false;
        const wrapper = shallow(<Tile tile={sampleTile} />);
        expect(wrapper.text().includes('SSO')).toBe(false);
    });
});
