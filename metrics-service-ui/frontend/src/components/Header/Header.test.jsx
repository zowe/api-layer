/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';

import Header from './Header';

describe('>>> Header component tests', () => {
    it('should display a service name', () => {
        const sample = shallow(<Header />);
        expect(sample.find('[id="name"]')).toExist();
    });

    it('should have href to itself', () => {
        const sample = shallow(<Header />);
        expect(sample.find('[id="name"]').prop('href')).toEqual('/metrics-service/ui/v1/#/dashboard');
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        const wrapper = shallow(<Header logout={logout} />);
        wrapper.find('[id="logout"]').simulate('click');
        expect(logout).toHaveBeenCalled();
    });
});
