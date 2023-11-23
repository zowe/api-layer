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
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Header from './Header';

describe('>>> Header component tests', () => {
    it('should display a Link', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('Link')).toBeDefined();
    });

    it('should have link href to itself', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('[data-testid="link"]').props().href).toEqual('#/dashboard');
    });

    it('should handle a Logout button click', () => {
        process.env.REACT_APP_API_PORTAL = false;
        const logout = jest.fn();
        const wrapper = enzyme.shallow(<Header logout={logout} />);
        wrapper.find('[data-testid="logout"]').simulate('click');
        expect(logout).toHaveBeenCalled();
    });

    it('should handle a Logout button click', () => {
        process.env.REACT_APP_API_PORTAL = false;
        const logout = jest.fn();
        render(<Header logout={logout} />);
        fireEvent.click(screen.getByTestId('logout-menu'));
        fireEvent.click(screen.getByText('Log out'));
        expect(logout).toHaveBeenCalled();
    });

    it('should create doc link', () => {
        const dummyTile = [
            {
                version: '1.0.0',
                id: 'apicatalog',
                title: 'API Mediation Layer API',
                status: 'UP',
                description: 'lkajsdlkjaldskj',
                customStyleConfig: {
                    docLink: 'doc|https://internal.com',
                },
            },
        ];
        process.env.REACT_APP_API_PORTAL = false;
        const wrapper = enzyme.shallow(<Header tiles={dummyTile} />);
        const link = wrapper.find('[data-testid="internal-link"]');
        expect(link.exists()).toEqual(true);
        expect(link.props().children.at(0)).toBe('doc');
        expect(link.props().href).toEqual('https://internal.com');
    });
});
