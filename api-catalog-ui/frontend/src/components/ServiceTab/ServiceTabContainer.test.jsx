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
import {HashRouter, Router} from 'react-router';
import ServiceTabContainer from './ServiceTabContainer';
import AppContainer from "../App/AppContainer";

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
                tiles,
                service: {
                    serviceId: 'service',
                }
            }
        });
        container = render(
            <HashRouter>
                <Provider store={store}>
                    <ServiceTabContainer tiles={tiles} />
                </Provider>
            </HashRouter>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
