/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import DetailPageContainer from './DetailPageContainer';

const mockStore = configureStore();

describe('DetailPage Container', () => {
    let store;
    let container;
    beforeEach(() => {
        const tiles = [
            {
                title: 'test',
                id: '2',
                description: 'test',
                services: [
                    {
                        serviceId: 'service',
                    },
                ],
            },
        ];
        store = mockStore({
            selectedServiceReducer: {
                selectedTile: 'tile',
                selectedService: {
                    serviceId: 'service',
                },
            },
            tilesReducer: {
                tile: 'tile',
                tiles,
                error: null,
            },
        });
        const match = {
            path: 'path',
            params: {
                serviceID: 'id',
            },
        };

        const history = {
            location: {
                pathname: '/service/serviceId',
            },
            push: jest.fn(),
            listen: jest.fn(),
        };
        container = render(
            <Provider store={store}>
                <DetailPageContainer match={match} history={history} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
