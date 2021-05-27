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
