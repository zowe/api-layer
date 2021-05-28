/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import { Router } from 'react-router-dom';
import { mount } from 'enzyme';
import { createStore } from 'redux';

import history from '../../helpers/history';
import { rootReducer } from '../../reducers';
import AuthRoute from './AuthRoute';

describe('>>> AuthRoute component tests', () => {
    let store;

    beforeEach(() => {
        store = createStore(rootReducer);
    });

    it('should contain a Redirect component when not authenticated', () => {
        const wrapper = mount(
            <Router history={history}>
                <AuthRoute store={store} />
            </Router>
        );
        expect(wrapper.find('AuthRoute').prop('authenticated')).toBeFalsy();
        expect(wrapper.find('Redirect')).toExist();
    });

    it('should contain a Route component when authenticated', () => {
        const wrapper = mount(
            <Router history={history}>
                <AuthRoute store={store} authenticated />
            </Router>
        );
        expect(wrapper.find('AuthRoute').prop('authenticated')).toBeTruthy();
        expect(wrapper.find('Route')).toExist();
    });
});
