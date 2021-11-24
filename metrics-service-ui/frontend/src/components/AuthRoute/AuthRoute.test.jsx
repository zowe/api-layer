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
import {act} from "react-dom/test-utils";

import history from '../../helpers/history';
import AuthRoute from './AuthRoute';

describe('>>> AuthRoute component tests', () => {
    it('should contain a Spinner component when waiting for authentication result', () => {
        const wrapper = mount(
          <Router history={history}>
              <AuthRoute />
          </Router>
      );
        expect(wrapper.find('Spinner')).toExist();
    });

    it('should contain a Redirect component when not authenticated', async () => {
        jest.spyOn(global, 'fetch').mockImplementation(() => Promise.reject({}));

        const wrapper = mount(
          <Router history={history}>
              <AuthRoute />
          </Router>
      );

        // awaits response from mocked fetch call
        await act(async () => {
            await new Promise(setImmediate);
            wrapper.update();
        });

        expect(wrapper.find('Redirect')).toExist();
    });

    it('should contain a Route component when authenticated', async () => {
        const mockResponse = {ok: true, text: () => Promise.resolve()};
        jest.spyOn(global, 'fetch').mockImplementation(() => Promise.resolve(mockResponse));

        const wrapper = mount(
          <Router history={history}>
              <AuthRoute />
          </Router>
        );

        // awaits response from mocked fetch call
        await act(async () => {
            await new Promise(setImmediate);
            wrapper.update();
        });
        expect(wrapper.find('Route')).toExist();
    });
});
