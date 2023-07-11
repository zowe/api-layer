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
import { Tab } from '@material-ui/core';
import { Link as RouterLink } from 'react-router-dom';
import SideBarLinks from './SideBarLinks';

const tile = {
    version: '1.0.0',
    id: 'apicatalog',
    title: 'API Mediation Layer for z/OS internal API services',
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
        },
        {
            serviceId: 'gateway',
            title: 'API Gateway',
            description:
                'API ML Microservice to locate and display API documentation for API ML discovered microservices',
            status: 'UP',
            secured: false,
        },
    ],
    totalServices: 1,
    activeServices: 1,
    lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
    createdTimestamp: '2018-08-22T08:31:22.948+0000',
};

describe('>>> SideBarLinks component tests', () => {
    it('should call storeCurrentTileId function when a tab is clicked', () => {
        const storeCurrentTileIdMock = jest.fn();
        const matchMock = { url: '/example' };
        const servicesMock = 'apicatalog';
        const wrapper = shallow(
            <SideBarLinks
                storeCurrentTileId={storeCurrentTileIdMock}
                originalTiles={[tile]}
                text="Tab Text"
                match={matchMock}
                services={servicesMock}
            />
        );

        wrapper.find(Tab).simulate('click');

        expect(storeCurrentTileIdMock).toHaveBeenCalledWith('apicatalog');
    });

    it('should render a Tab component with the correct props', () => {
        const storeCurrentTileIdMock = jest.fn();
        const matchMock = { url: '/example' };
        const servicesMock = 'service1';
        const wrapper = shallow(
            <SideBarLinks
                storeCurrentTileId={storeCurrentTileIdMock}
                originalTiles={[tile]}
                text="Tab Text"
                match={matchMock}
                services={servicesMock}
            />
        );

        expect(wrapper.find(Tab)).toHaveLength(1);

        expect(wrapper.find(Tab).props()).toMatchObject({
            onClick: expect.any(Function),
            value: 'Tab Text',
            className: 'tabs',
            component: RouterLink,
            to: '/example/service1',
            label: 'Tab Text',
            wrapped: true,
        });
    });
});
