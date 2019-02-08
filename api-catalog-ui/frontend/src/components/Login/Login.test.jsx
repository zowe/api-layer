/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import * as enzyme from 'enzyme';
import jest from 'jest-mock';
import Login from './Login';

describe('>>> Login page component tests', () => {
    it('should submit username and password input', () => {
        const loginMock = jest.fn();

        const page = enzyme.shallow(<Login login={loginMock} />);
        page.find('TextInput')
            .first()
            .simulate('change', { target: { name: 'username', value: 'user' } });

        page.find('TextInput')
            .last()
            .simulate('change', { target: { name: 'password', value: 'password' } });

        page.find('form').simulate('submit', {
            preventDefault: () => {},
        });

        expect(loginMock).toHaveBeenCalled();
    });

    it('should display message if username and password are empty and submited', () => {
        const page = enzyme.shallow(<Login />);

        page.find('TextInput')
            .first()
            .simulate('change', { target: { name: 'username', value: '' } });

        page.find('TextInput')
            .last()
            .simulate('change', { target: { name: 'password', value: '' } });

        const button = page.find('Button');
        button.simulate('click');
        const errorMessage = page.find('p.error-message-content');
        expect(button).toBeDefined();
        expect(errorMessage).toBeDefined();
    });

    it('should enable login button if username and password are populated', () => {
        const page = enzyme.shallow(<Login />);

        page.find('TextInput')
            .first()
            .simulate('change', { target: { name: 'username', value: 'user' } });

        page.find('TextInput')
            .last()
            .simulate('change', { target: { name: 'password', value: 'password' } });

        const button = page.find('Button');
        expect(button).toBeDefined();
        expect(button.props().disabled).toBeFalsy();
    });

    it('should render form', () => {
        const page = enzyme.shallow(<Login />);

        expect(page.find('form')).toBeDefined();
    });

    it('should display a credentials failure message', () => {
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError({
            messageType: 'ERROR',
            messageNumber: 'SEC0005',
            messageContent:
                "Authentication problem: 'Username or password are invalid.' for URL '/apicatalog/auth/login'",
            messageKey: 'com.ca.mfaas.security.invalidUsername',
        });
        expect(messageText).toEqual('Username or password is invalid.');
    });

    it('should display a no credentials message', () => {
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError({
            messageType: 'ERROR',
            messageNumber: 'UI0001',
            message: 'Please provide a valid username and password',
        });
        expect(messageText).toEqual('Please provide a valid username and password');
    });

    it('should display authetication required', () => {
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError({
            messageType: 'ERROR',
            messageNumber: 'SEC0001',
            messageContent:
                "Authentication problem: 'Username or password are invalid.' for URL '/apicatalog/auth/login'",
            messageKey: 'com.ca.mfaas.security.authenticationRequired',
        });
        expect(messageText).toEqual('Authentication is required.');
    });

    it('should display session has expired', () => {
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError({
            messageType: 'ERROR',
            messageNumber: 'SEC0004',
            messageContent:
                "Authentication problem: 'Username or password are invalid.' for URL '/apicatalog/auth/login'",
            messageKey: 'com.ca.mfaas.security.sessionExpired',
        });
        expect(messageText).toEqual('Session has expired, please login again.');
    });

    it('should display server generated failure message', () => {
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError({
            messageType: 'ERROR',
            messageNumber: 'SEC00099',
            messageContent:
                "Authentication problem: 'Username or password are invalid.' for URL '/apicatalog/auth/login'",
            messageKey: 'com.ca.mfaas.security.otherError',
        });
        expect(messageText).toEqual(`Authentication Error: SEC00099. Try to log in again.`);
    });

    it('should disable button and show spinner when request is being resolved', () => {
        const wrapper = enzyme.shallow(<Login isFetching />);

        const submitButton = wrapper.find('Button');
        const spinner = wrapper.find('Spinner');

        expect(submitButton).toBeDefined();
        expect(spinner).toBeDefined();
        expect(submitButton.props().disabled).toBeTruthy();
    });

    it('should display UI errorMessage', () => {
        const page = enzyme.shallow(<Login errorMessage="Cus bus" />);
        const errorMessage = page.find('p.error-message-content').first();

        expect(errorMessage).toBeDefined();
    });
});
