/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import * as enzyme from 'enzyme';
import Footer from './Footer';

describe('>>> Footer component tests', () => {
    it('should display a Link', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link')).toBeDefined();
    });

    it('should have link href to CA support', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(
            footer
                .find('Link')
                .first()
                .props().href
        ).toEqual('https://support.ca.com/us.html');
    });

    it('should not display link to ca support', () => {
        process.env.REACT_APP_CA_ENV = false;
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link')).toBeUndefined();
    });
});
