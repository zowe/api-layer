/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import * as enzyme from 'enzyme';
import Footer from './Footer';

describe('>>> Footer component tests', () => {
    it('should not display a Link', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('footer').length).toBeFalsy();
    });

    it('should display link to ca support', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link').length).toBeDefined();
    });

    it('should have link href to CA support', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        expect(
            footer
                .find('Link')
                .first()
                .props().href
        ).toEqual('https://support.ca.com/us.html');
    });
});
