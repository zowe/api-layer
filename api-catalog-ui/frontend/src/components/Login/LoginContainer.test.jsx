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
import LoginContainer from './LoginContainer';

const mockStore = configureStore();

describe('Login Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore({
            authenticationReducer: {},
        });
        container = render(
            <HashRouter>
                <Provider store={store}>
                    <LoginContainer />
                </Provider>
            </HashRouter>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
