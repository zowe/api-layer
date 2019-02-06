import React from 'react';
import { Button, FormField, TextInput } from 'mineral-ui';
import { IconDanger } from 'mineral-ui-icons';

import logoImage from '../../assets/images/api-catalog-logo.png';
import './Login.css';
import './LoginWebflow.css';
import Spinner from '../Spinner/Spinner';

export default class Login extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            username: '',
            password: '',
        };

        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    onLoad(feedItem) {
        this.setState(({ loadedItems }) => ({ loadedItems: loadedItems.concat(feedItem) }));
    }

    isDisabled = () => {
        const { username, password } = this.state;
        const { isFetching } = this.props;
        return !(username.trim().length > 0 && password.trim().length > 0 && !isFetching);
    };

    handleError = error => {
        let messageText;
        if (
            error.messageNumber !== undefined &&
            error.messageNumber !== null &&
            error.messageType !== undefined &&
            error.messageType !== null
        ) {
            switch (error.messageNumber) {
                case 'SEC0001':
                    messageText = `Authentication is required. (${error.messageNumber})`;
                    break;
                case 'SEC0004':
                    messageText = `Session has expired, please login again. (${error.messageNumber})`;
                    break;
                case 'SEC0005':
                    messageText = `Username or password is invalid. (${error.messageNumber})`;
                    break;
                case 'MFS0104':
                    messageText = `The request is taking too long. Please try again later. (${error.messageNumber})`;
                    break;
                default:
                    messageText = `Authentication Error: ${error.messageNumber}. Try to log in again.`;
                    break;
            }
        }
        return messageText;
    };

    handleChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    }

    handleSubmit(e) {
        e.preventDefault();

        const { username, password } = this.state;
        const { login } = this.props;
        if (username && password) {
            login({ username, password });
        }
    }

    render() {
        const { username, password } = this.state;
        const { authentication, isFetching } = this.props;
        let messageText;
        if (
            authentication !== undefined &&
            authentication !== null &&
            authentication.error !== undefined &&
            authentication.error !== null
        ) {
            messageText = this.handleError(authentication.error);
        }
        return (
            <div className="login-object">
                <div className="login-form">
                    {' '}
                    <div className="title-container">
                        <div className="logo-container">
                            <img src={logoImage} alt="" />
                        </div>
                        <div className="product-title">
                            <div className="text-block-2">API Catalog</div>
                        </div>
                    </div>
                    <div className="login-inputs-container">
                        <div className="username-container">
                            <div className="username-input">
                                <div className="w-form">
                                    <form
                                        id="login-form"
                                        name="login-form"
                                        data-testid="login-form"
                                        data-name="Login Form"
                                        className="form"
                                        onSubmit={this.handleSubmit}
                                    >
                                        <FormField label="Username" className="formfield">
                                            <TextInput
                                                id="username"
                                                data-testid="username"
                                                name="username"
                                                type="text"
                                                size="jumbo"
                                                value={username}
                                                onChange={this.handleChange}
                                            />
                                        </FormField>
                                        <FormField label="Password" className="formfield">
                                            <TextInput
                                                id="password"
                                                data-testid="password"
                                                name="password"
                                                type="password"
                                                size="jumbo"
                                                value={password}
                                                onChange={this.handleChange}
                                                caption="Default: password"
                                            />
                                        </FormField>
                                        <FormField className="formfield" label="">
                                            <Button
                                                type="submit"
                                                data-testid="submit"
                                                primary
                                                fullWidth
                                                disabled={this.isDisabled()}
                                                size="jumbo"
                                            >
                                                Sign in
                                            </Button>
                                        </FormField>
                                        <FormField className="formfield form-spinner" label="">
                                            <Spinner
                                                isLoading={isFetching}
                                                css={{
                                                    position: 'relative',
                                                    top: '70px',
                                                }}
                                            />
                                        </FormField>
                                        {messageText !== undefined &&
                                            messageText !== null && (
                                                <FormField className="error-message" label="">
                                                    <div id="error-message">
                                                        <p className="error-message-content">
                                                            <IconDanger color="#de1b1b" size="2rem" /> {messageText}
                                                        </p>
                                                    </div>
                                                </FormField>
                                            )}
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}
