/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import Footer from './Footer';

describe('>>> Footer component tests', () => {
    it('should not display a Link', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('footer').length).toBeFalsy();
    });

    it('should display link to ca support', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link').length).toBeDefined();
    });

    it('should have link href to CA support', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link').first().props().href).toEqual('https://support.ca.com/us.html');
    });

    it('should show the copyright', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        const copyright = footer.find('p').text();
        expect(copyright).toBe(
            'Copyright © 2019 Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.'
        );
    });
});
