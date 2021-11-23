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

import Header from './Header';

describe('>>> Header component tests', () => {
    it('should display a service name', () => {
        const wrapper = shallow(<Header />);
        expect(wrapper.find('WithStyles(WithStyles(ForwardRef(Link)))')).toExist();
    });

    it('should have href to itself', () => {
        const wrapper = shallow(<Header />);
        expect(wrapper.find('WithStyles(WithStyles(ForwardRef(Link)))').prop('href')).toEqual(
            '/metrics-service/ui/v1/#/dashboard'
        );
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        const wrapper = shallow(<Header logout={logout} />);
        wrapper.find('WithStyles(WithStyles(ForwardRef(IconButton)))').simulate('click');
        expect(logout).toHaveBeenCalled();
    });

    it('should have a tooltip for the logout icon button', () => {
        const wrapper = shallow(<Header />);
        expect(wrapper.find('WithStyles(ForwardRef(Tooltip))')).toExist();
    });
});
