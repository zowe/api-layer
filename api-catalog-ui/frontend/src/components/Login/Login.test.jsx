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

    it('should disable login button if username and password are empty', () => {
        const page = enzyme.shallow(<Login />);

        page.find('TextInput')
            .first()
            .simulate('change', { target: { name: 'username', value: '' } });

        page.find('TextInput')
            .last()
            .simulate('change', { target: { name: 'password', value: '' } });

        const button = page.find('Button');
        expect(button).toBeDefined();
        expect(button.props().disabled).toBeTruthy();
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

    it('should display an credentials failure message', () => {
        const authentication = {
            error: {
                messageNumber: 'SEC0005',
                messageType: 'ERROR',
                messageContent: 'Should not be displayed',
            },
        };
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError(authentication);
        expect(messageText).toEqual('Username or password is invalid');
    });

    it('should display server generated failure message', () => {
        const authentication = {
            error: {
                messageNumber: 'SEC0003',
                messageType: 'ERROR',
                messageContent: 'Should be displayed',
            },
        };
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError(authentication);
        expect(messageText).toEqual(`Internal Error: ${authentication.error.messageNumber}`);
    });

    it('should display server generated ajax message', () => {
        const authentication = {
            error: {
                name: 'AjaxError',
                response: {
                    messages: [
                        {
                            messageNumber: 'SEC0099',
                            messageType: 'ERROR',
                            messageContent: 'Should be displayed',
                        },
                    ],
                },
                status: 401,
            },
        };
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError(authentication);
        expect(messageText).toEqual('Internal Error: SEC0099');
    });

    it('should display UI session ajax message', () => {
        const authentication = {
            error: {
                name: 'AjaxError',
                response: {
                    messages: [
                        {
                            messageNumber: 'SEC0004',
                            messageType: 'ERROR',
                            messageContent: 'Should not be displayed',
                        },
                    ],
                },
                status: 401,
            },
        };
        const wrapper = enzyme.shallow(<Login />);
        const instance = wrapper.instance();
        const messageText = instance.handleError(authentication);
        expect(messageText).toEqual('Session has expired, please login again');
    });
});
