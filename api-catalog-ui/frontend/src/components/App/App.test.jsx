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
import { render, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router';
import App from './App';
import { act } from 'react-dom/test-utils';
import { userService } from "../../services";

const mockNavigate = jest.fn();
const mockLocation = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
        useLocation: () => mockLocation,
    };
});

jest.mock('../../services/user.service');

async function assertMethod(mockSuccess) {
    await act(async () => {
        render(
            <MemoryRouter initialEntries={['/dashboard']}>
                <App authentication={{user: null}} success={mockSuccess}/>
            </MemoryRouter>
        );
    });

    await waitFor(() => {
        expect(userService.query).toHaveBeenCalled();
        expect(mockSuccess).not.toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
}

describe('>>> App component tests', () => {
    it('should call render', () => {
        const history = { push: jest.fn() };
        const success = { push: jest.fn() };
        const authentication = { user: 'user' };
        const { getByText } = render(<App history={history} success={success} authentication={authentication} />);

        expect(getByText(/Go to Dashboard/i)).toBeInTheDocument();
    });

    it('should call render when portal enabled', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const history = { push: jest.fn() };
        const success = { push: jest.fn() };
        const authentication = { user: 'user' };
        const { getByText } = render(<App history={history} success={success} authentication={authentication}/>);

        expect(getByText(/Go to Dashboard/i)).toBeInTheDocument();
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

    it('calls success when userService.query returns 200', async () => {
        const mockSuccess = jest.fn();

        userService.query.mockResolvedValue({ status: 200, userId: 'mockUser' });

        await act(async () => {
            render(
                <MemoryRouter initialEntries={['/dashboard']}>
                    <App authentication={{ user: null }} success={mockSuccess} />
                </MemoryRouter>
            );
        });

        await waitFor(() => {
            expect(userService.query).toHaveBeenCalled();
            expect(mockSuccess).toHaveBeenCalledWith('mockUser', false);
        });
    });

    it('navigates to /login when userService.query returns non-200', async () => {
        const mockSuccess = jest.fn();

        userService.query.mockResolvedValue({ status: 401 });

        await assertMethod(mockSuccess);
    });

    it('navigates to /login when userService.query throws error', async () => {
        const mockSuccess = jest.fn();

        userService.query.mockRejectedValue(new Error('Network error'));
        await assertMethod(mockSuccess);
    });
});
