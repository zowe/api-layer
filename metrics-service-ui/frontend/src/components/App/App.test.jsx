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

import React from 'react';
import { shallow } from 'enzyme';
import App from './App';

describe('>>> App component tests', () => {
    it('should call render', () => {
        const history = { push: jest.fn() };
        const wrapper = shallow(<App history={history} />);
        const instance = wrapper.instance();
        expect(instance).not.toBeNull();
    });

    it('should not show header on login route', () => {
        const history = { push: jest.fn() };
        const wrapper = shallow(<App history={history} />);
        const header = wrapper.find('.header');

        expect(header).toHaveLength(0);
    });
});
