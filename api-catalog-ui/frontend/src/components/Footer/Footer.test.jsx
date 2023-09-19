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
    xit('should not display a Link', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('footer').length).toBeFalsy();
    });

    xit('should display link', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('Link').length).toBeDefined();
    });

    xit('should show the paragraph', () => {
        process.env.REACT_APP_CA_ENV = true;
        const footer = enzyme.shallow(<Footer />);
        const paragraph = footer.find('p');
        expect(paragraph).toExist();
    });
});
