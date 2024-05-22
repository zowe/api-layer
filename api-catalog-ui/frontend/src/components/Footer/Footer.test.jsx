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
    it('should display test build info', () => {
        const footer = enzyme.shallow(<Footer />);
        expect(footer.find('footer').length).toBeDefined();
        expect(footer.find('footer').contains('Version: test build info'));
    });
});
