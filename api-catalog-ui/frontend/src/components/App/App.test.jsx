/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { shallow } from 'enzyme';
import { fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

describe('>>> App component tests', () => {
    beforeEach(() => {
        process.env.REACT_APP_API_PORTAL = false;
    });

    it('should call render', () => {
        const history = { push: jest.fn() };
        const wrapper = shallow(<App history={history} />);
        const instance = wrapper.instance();
        expect(instance).not.toBeNull();
    });

    it('should not show header on login route', () => {
        const wrapper = shallow(
            <MemoryRouter initialEntries={['/login']}>
                <App />
            </MemoryRouter>
        );
        const header = wrapper.find('.header');

        expect(header).toHaveLength(0);
    });

    it('should find the dashboard-mobile div', () => {
        process.env.REACT_APP_API_PORTAL = 'true';
        const wrapper = shallow(<App />);
        const header = wrapper.find('.dashboard-mobile-menu');

        expect(header.exists()).toEqual(true);
    });

    it('should not find the dashboard-mobile div if it is not api portal', () => {
        process.env.REACT_APP_API_PORTAL = false;
        const wrapper = shallow(<App />);
        const header = wrapper.find('.dashboard-mobile-menu mobile-view');

        expect(header.exists()).toEqual(false);
    });

    it('should resize window', () => {
        process.env.REACT_APP_API_PORTAL = true;
        // eslint-disable-next-line global-require
        const utils = require('../../utils/utilFunctions');
        const spyCloseMobileMenu = jest.spyOn(utils, 'closeMobileMenu');
        window.innerWidth = 800;
        fireEvent(window, new Event('resize'));
        waitFor(() => {
            expect(spyCloseMobileMenu).toHaveBeenCalled();
        });
    });
});
