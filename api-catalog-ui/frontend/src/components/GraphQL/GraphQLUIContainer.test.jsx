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
import GraphQLContainer from './GraphQLUIApimlContainer';

const mockStore = configureStore();

describe('GraphQL Container', () => {
    let store;
    let container;

    // mock codemirror getBoundingClientRect()
    document.createRange = () => {
        const range = new Range();
        range.getBoundingClientRect = jest.fn();
        range.getClientRects = jest.fn(() => ({
            item: () => null,
            length: 0,
        }));
        return range;
    };

    beforeEach(() => {
        store = mockStore({
            selectedServiceReducer: {
                selectedService: {
                    apis: [],
                    serviceId: 'service',
                },
            },
            tilesReducer: {
                tiles: [{}],
            },
        });
        container = render(
            <Provider store={store}>
                <GraphQLContainer graphqlUrl="http://localhost:4000/graphql" />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
