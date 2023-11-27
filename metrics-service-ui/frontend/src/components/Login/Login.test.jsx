/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import React from 'react';
import { shallow } from 'enzyme';
import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import Login from './Login';

jest.mock(
    '../Icons/MetricsIconButton.jsx',
    () =>
        // eslint-disable-next-line react/display-name
        function () {
            const MetricsIconButton = 'MetricsIconButtonMock';
            return <MetricsIconButton />;
        }
);

describe('>>> Login page component tests', () => {
    const TextField = 'WithStyles(WithStyles(ForwardRef(TextField)))';

    it('should submit username and password input', () => {
        const loginMock = jest.fn();

        const page = shallow(<Login login={loginMock} />);
        page.find(TextField)
            .first()
            .simulate('change', { target: { name: 'username', value: 'user' } });

        page.find(TextField)
            .last()
            .simulate('change', { target: { name: 'password', value: 'password' } });

        page.find('form').simulate('submit', {
            preventDefault: () => {},
        });

        expect(loginMock).toHaveBeenCalled();
    });

    describe('should not submit login request if invalid form input', () => {
        const preventDefault = jest.fn();
        let loginMock;

        beforeEach(() => {
            loginMock = jest.fn();
        });

        it('username and password empty', () => {
            const page = shallow(<Login login={loginMock} />);

            page.find(TextField)
                .first()
                .simulate('change', { target: { name: 'username', value: '' } });

            page.find(TextField)
                .last()
                .simulate('change', { target: { name: 'password', value: '' } });

            const button = page.find('[id="submit"]');
            expect(button).toExist();

            button.simulate('click', { preventDefault });
            expect(loginMock).not.toHaveBeenCalled();
        });

        it('username empty', () => {
            const page = shallow(<Login login={loginMock} />);

            page.find(TextField)
                .first()
                .simulate('change', { target: { name: 'username', value: '' } });

            page.find(TextField)
                .last()
                .simulate('change', { target: { name: 'password', value: 'password' } });

            const button = page.find('[id="submit"]');
            expect(button).toExist();

            button.simulate('click', { preventDefault });
            expect(loginMock).not.toHaveBeenCalled();
        });

        it('password empty', () => {
            const page = shallow(<Login login={loginMock} />);

            page.find(TextField)
                .first()
                .simulate('change', { target: { name: 'username', value: 'user' } });

            page.find(TextField)
                .last()
                .simulate('change', { target: { name: 'password', value: '' } });

            const button = page.find('[id="submit"]');
            expect(button).toExist();

            button.simulate('click', { preventDefault });
            expect(loginMock).not.toHaveBeenCalled();
        });
    });

    it('should enable login button if username and password are populated', () => {
        const page = shallow(<Login />);

        page.find(TextField)
            .first()
            .simulate('change', { target: { name: 'username', value: 'user' } });

        page.find(TextField)
            .last()
            .simulate('change', { target: { name: 'password', value: 'password' } });

        const button = page.find('[id="submit"]');
        expect(button).toExist();
        expect(button.props().disabled).toBeFalsy();
    });

    it('should render form', () => {
        const page = shallow(<Login />);

        expect(page.find('form')).toExist();
    });

    describe('should display error message', () => {
        it('credentials failure message', () => {
            const error = {
                messageType: 'ERROR',
                messageNumber: 'ZWEAS120E',
                messageContent: "Authentication problem: 'Invalid Credentials' for URL '/metrics-service/auth/login'",
                messageKey: 'org.zowe.apiml.security.invalidUsername',
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAS120E) Invalid username or password'
            );
        });

        it('should display a 401 failure message', () => {
            render(
                <Login
                    authentication={{
                        onCompleteHandling: jest.fn(),
                        sessionOn: true,
                        error: {
                            status: 401,
                        },
                    }}
                />
            );
            expect(screen.getByText('(ZWEAS102E) Session has expired, please login again')).toBeInTheDocument();
        });

        it('authentication service not available message', () => {
            const error = {
                messageType: 'ERROR',
                messageNumber: 'ZWEAS104E',
                messageContent: 'Authentication service is not available by URL',
                messageKey: 'org.zowe.apiml.security.authenticationRequired',
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAS104E) Authentication service not available, please try again later'
            );
        });

        it('session has expired', () => {
            const error = {
                messageType: 'ERROR',
                messageNumber: 'ZWEAS102E',
                messageContent: 'Token is expired for URL',
                messageKey: 'org.zowe.apiml.security.expiredToken',
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAS102E) Session has expired, please login again'
            );
        });

        it('generic failure message', () => {
            const error = {
                messageType: 'ERROR',
                messageNumber: 'ZWEAS100E',
                messageContent: 'Authentication exception for URL',
                messageKey: 'org.zowe.apiml.security.generic',
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAS100E) A generic failure occurred while authenticating'
            );
        });

        it('500 status', () => {
            const error = {
                status: 500,
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAS100E) A generic failure occurred while authenticating'
            );
        });

        it('request timeout message', () => {
            const error = {
                messageType: 'ERROR',
                messageNumber: 'ZWEAM700E',
                messageContent: 'No response received within the allowed time',
                messageKey: 'org.zowe.apiml.common.serviceTimeout',
            };
            const wrapper = shallow(<Login authentication={{ error }} />);

            expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
                '(ZWEAM700E) Request timeout, please try again later'
            );
        });

        it('properties error message', () => {
            const page = shallow(<Login errorText="text" />);
            const errorMessage = page.find('[id="errormessage"]');

            expect(errorMessage).toExist();
        });
    });

    it('should not display UI error message', () => {
        const page = shallow(<Login />);
        const errorMessage = page.find('[id="errormessage"]');

        expect(errorMessage).not.toExist();
    });

    it('should disable button and show spinner when request is being resolved', () => {
        const wrapper = shallow(<Login isFetching />);

        const submitButton = wrapper.find('[id="submit"]');
        const spinner = wrapper.find('Spinner');

        expect(submitButton).toExist();
        expect(spinner).toExist();
        expect(submitButton.props().disabled).toBeTruthy();
    });
});
