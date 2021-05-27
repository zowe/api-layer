/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';
import jest from 'jest-mock';

import Login from './Login';

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

    it('should not submit login request if username and password empty', () => {
        const loginMock = jest.fn();
        const page = shallow(<Login login={loginMock} />);

        page.find(TextField)
            .first()
            .simulate('change', { target: { name: 'username', value: '' } });

        page.find(TextField)
            .last()
            .simulate('change', { target: { name: 'password', value: '' } });

        const button = page.find('[id="submit"]');
        expect(button).toExist();

        button.simulate('click', { preventDefault: jest.fn() });
        expect(loginMock).not.toHaveBeenCalled();
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

    it('should display a credentials failure message', () => {
        const error = {
            messageType: 'ERROR',
            messageNumber: 'ZWEAS120E',
            messageContent:
                "Authentication problem: 'Username or password are invalid.' for URL '/metrics-service/auth/login'",
            messageKey: 'org.zowe.apiml.security.invalidUsername',
        };
        const wrapper = shallow(<Login authentication={{ error }} />);

        expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual('(ZWEAS120E) Invalid username or password');
    });

    it('should display authetication service not available message', () => {
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

    it('should display session has expired', () => {
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

    it('should display generic failure message', () => {
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

    it('should display generic failure message because of 500 status', () => {
        const error = {
            status: 500,
        };
        const wrapper = shallow(<Login authentication={{ error }} />);

        expect(wrapper.find('[id="errormessage"]').prop('text')).toEqual(
            '(ZWEAS100E) A generic failure occurred while authenticating'
        );
    });

    it('should display request timeout message', () => {
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

    it('should disable button and show spinner when request is being resolved', () => {
        const wrapper = shallow(<Login isFetching />);

        const submitButton = wrapper.find('[id="submit"]');
        const spinner = wrapper.find('Spinner');

        expect(submitButton).toExist();
        expect(spinner).toExist();
        expect(submitButton.props().disabled).toBeTruthy();
    });

    it('should display UI error message', () => {
        const page = shallow(<Login errorText="text" />);
        const errorMessage = page.find('[id="errormessage"]');

        expect(errorMessage).toExist();
    });

    it('should not display UI error message', () => {
        const page = shallow(<Login />);
        const errorMessage = page.find('[id="errormessage"]');

        expect(errorMessage).not.toExist();
    });
});
