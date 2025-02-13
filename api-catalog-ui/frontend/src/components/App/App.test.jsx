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
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router';
import App from './App';

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
describe('>>> App component tests', () => {
    it('should call render', () => {
        const history = { push: jest.fn() };
        const { getByText } = render(<App history={history} />);

        expect(getByText(/Go to Dashboard/i)).toBeInTheDocument();
    });

    it('should call render when portal enabled', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const history = { push: jest.fn() };
        const { getByText } = render(<App history={history} />);

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
});
