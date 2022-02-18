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
import '@testing-library/jest-dom/extend-expect';
import Header from './Header';

describe('>>> Header component tests', () => {
    it('should display a Link', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('Link')).toBeDefined();
    });

    it('should have link href to itself', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('[data-testid="link"]').props().href).toEqual('ui/v1/apicatalog/#/dashboard');
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        const wrapper = enzyme.shallow(<Header logout={logout} />);
        wrapper.find('[data-testid="logout"]').simulate('click');
        expect(logout).toHaveBeenCalled();
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        render(<Header logout={logout} />);
        fireEvent.click(screen.getByTestId('logout-menu'));
        fireEvent.click(screen.getByText('Log out'));
        expect(logout).toHaveBeenCalled();
    });
});
