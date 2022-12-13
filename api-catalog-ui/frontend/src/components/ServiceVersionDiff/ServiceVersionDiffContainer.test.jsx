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
import ServiceVersionDiffContainer from './ServiceVersionDiffContainer';

const mockStore = configureStore();

describe('ServiceVersionDiff Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore({
            serviceVersionDiff: {
                diffText: 'diffText',
                oldVersion: '1.0',
                newVersion: '2.0',
            },
        });
        container = render(
            <Provider store={store}>
                <ServiceVersionDiffContainer versions={['v1', 'v2']} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
