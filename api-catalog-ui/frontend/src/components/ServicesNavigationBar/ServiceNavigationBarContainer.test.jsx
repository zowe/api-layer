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
import '@testing-library/jest-dom/extend-expect';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import { Router } from 'react-router-dom';
import ServicesNavigationBarContainer from './ServicesNavigationBarContainer';

const mockStore = configureStore();

describe('ServiceTab Container', () => {
    let store;
    let container;
    beforeEach(() => {
        const tiles = [
            {
                title: 'test',
                id: '2',
                description: 'test',
                services: [{ id: 'service1', apiVersions: ['org.zowe v1', 'org.zowe v2'] }],
            },
        ];
        store = mockStore({
            tilesReducer: {
                services: tiles.services,
                currentTileId: 'id',
            },
            filtersReducer: {
                text: 'test',
            },
        });
        const history = {
            location: {
                pathname: {},
            },
            push: jest.fn(),
            listen: jest.fn(),
        };
        container = render(
            <Router history={history}>
                <Provider store={store}>
                    <ServicesNavigationBarContainer services={tiles.services} />
                </Provider>
            </Router>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
