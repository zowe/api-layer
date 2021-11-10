import React from 'react';
import { IconButton, InputAdornment, InputLabel, OutlinedInput } from '@material-ui/core';
import Button from '@material-ui/core/Button';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import WarningIcon from '@material-ui/icons/Warning';
import logoImage from '../../assets/images/api-catalog-logo.png';
import Spinner from '../Spinner/Spinner';
import './Login.css';

export default class Login extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            username: '',
            password: '',
            errorMessage: '',
            showPassword: false,
        };

        this.handleClickShowPassword = this.handleClickShowPassword.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleClickShowPassword(showPassword) {
        this.setState({ showPassword: !showPassword });
        /* eslint-disable no-console */
        console.log(showPassword);
    }

    isDisabled = () => {
        const { isFetching } = this.props;
        return isFetching;
    };

    handleError = error => {
        let messageText;
        const { authentication } = this.props;
        // eslint-disable-next-line global-require
        const errorMessages = require('../../error-messages.json');
        if (
            error.messageNumber !== undefined &&
            error.messageNumber !== null &&
            error.messageType !== undefined &&
            error.messageType !== null
        ) {
            messageText = `Unexpected error, please try again later (${error.messageNumber})`;
            const filter = errorMessages.messages.filter(
                x => x.messageKey != null && x.messageKey === error.messageNumber
            );
            if (filter.length !== 0) messageText = `(${error.messageNumber}) ${filter[0].messageText}`;
        } else if (error.status === 401 && authentication.sessionOn) {
            messageText = `(${errorMessages.messages[0].messageKey}) ${errorMessages.messages[0].messageText}`;
            authentication.onCompleteHandling();
        } else if (error.status === 500) {
            messageText = `(${errorMessages.messages[1].messageKey}) ${errorMessages.messages[1].messageText}`;
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
        const { username, password, errorMessage, showPassword } = this.state;
        const { authentication, isFetching } = this.props;
        let messageText;
        if (
            authentication !== undefined &&
            authentication !== null &&
            authentication.error !== undefined &&
            authentication.error !== null
        ) {
            messageText = this.handleError(authentication.error);
        } else if (errorMessage) {
            messageText = errorMessage;
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
                                        <CssBaseline />
                                        <TextField
                                            label="Username"
                                            data-testid="username"
                                            className="formfield"
                                            variant="outlined"
                                            // margin="normal"
                                            required
                                            fullWidth
                                            id="email"
                                            name="username"
                                            value={username}
                                            onChange={this.handleChange}
                                            autoComplete="on"
                                            // autoFocus
                                        />
                                        <InputLabel className="formfield" htmlFor="outlined-adornment-password">
                                            Password
                                        </InputLabel>
                                        <OutlinedInput
                                            id="password"
                                            data-testid="password"
                                            name="password"
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onChange={this.handleChange}
                                            caption="Default: password"
                                            autoComplete="on"
                                            endAdornment={
                                                <InputAdornment position="end">
                                                    <IconButton
                                                        aria-label="toggle password visibility"
                                                        edge="end"
                                                        onClick={() => this.handleClickShowPassword(showPassword)}
                                                    >
                                                        {showPassword ? <VisibilityOff /> : <Visibility />}
                                                    </IconButton>
                                                </InputAdornment>
                                            }
                                            label="Password"
                                        />
                                        <Button
                                            className="formfield"
                                            label=""
                                            type="submit"
                                            data-testid="submit"
                                            primary
                                            fullWidth
                                            disabled={this.isDisabled()}
                                            style={{ size: 'jumbo' }}
                                        >
                                            Sign in
                                        </Button>
                                        <Spinner
                                            className="formfield"
                                            isLoading={isFetching}
                                            css={{
                                                position: 'relative',
                                                top: '70px',
                                            }}
                                        />
                                        {messageText !== undefined &&
                                            messageText !== null && (
                                                <div id="error-message">
                                                    <p style={{ color: '#de1b1b' }} className="error-message-content">
                                                        <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                                        {messageText}
                                                    </p>
                                                </div>
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
