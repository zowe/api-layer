/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import * as enzyme from 'enzyme';
import Footer from './Footer';

describe('>>> Footer component tests', () => {
    it('should display a Link', () => {
        const sample = enzyme.shallow(<Footer />);
        expect(sample.find('Link')).toBeDefined();
    });

    it('should have link href to CA support', () => {
        const sample = enzyme.shallow(<Footer />);
        expect(sample.find('Link').first().props().href).toEqual('https://support.ca.com/us.html');
    });
});
