import React from 'react';
import { IconButton, InputAdornment, Typography, Button, CssBaseline, TextField, Link } from '@material-ui/core';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import WarningIcon from '@material-ui/icons/Warning';
import ErrorOutlineIcon from '@material-ui/icons/ErrorOutline';
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
        this.onKeyDown = this.onKeyDown.bind(this);
    }

    /**
     * Detect caps lock being on when typing.
     * @param keyEvent On key down event.
     */
    onKeyDown = keyEvent => {
        this.setState({ warning: false });
        if (keyEvent.getModifierState('CapsLock')) {
            this.setState({ warning: true });
        } else {
            this.setState({ warning: false });
        }
    };

    handleClickShowPassword(showPassword) {
        this.setState({ showPassword: !showPassword });
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
            if (filter.length !== 0) {
                if (filter[0].messageKey === 'ZWEAS120E') {
                    messageText = `${filter[0].messageText}`;
                } else {
                    messageText = `(${error.messageNumber}) ${filter[0].messageText}`;
                }
            }
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
        const { username, password, errorMessage, showPassword, warning } = this.state;
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
                                        <div className="text-block-4">API Catalog</div>
                                        <br />
                                        {messageText !== undefined &&
                                            messageText !== null && (
                                                <div id="error-message">
                                                    <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                                    {messageText}
                                                </div>
                                            )}
                                        <Typography
                                            className="login-typo"
                                            variant="subtitle1"
                                            gutterBottom
                                            component="div"
                                        >
                                            Login
                                        </Typography>
                                        <br />
                                        <Typography variant="subtitle2" gutterBottom component="div">
                                            Please enter your mainframe username and password to access this resource
                                        </Typography>
                                        <br />
                                        <TextField
                                            label="Username"
                                            data-testid="username"
                                            className="formfield"
                                            variant="outlined"
                                            required
                                            error={!!messageText}
                                            fullWidth
                                            id="username"
                                            name="username"
                                            value={username}
                                            onChange={this.handleChange}
                                            autoComplete="on"
                                            autoFocus
                                        />
                                        <br />
                                        <br />
                                        <br />
                                        <TextField
                                            id="password"
                                            htmlFor="outlined-adornment-password"
                                            label="Password"
                                            data-testid="password"
                                            className="formfield"
                                            variant="outlined"
                                            required
                                            error={!!messageText}
                                            fullWidth
                                            name="password"
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onKeyDown={this.onKeyDown}
                                            onChange={this.handleChange}
                                            caption="Default: password"
                                            autoComplete="on"
                                            InputProps={{
                                                endAdornment: (
                                                    <InputAdornment position="end">
                                                        <IconButton
                                                            aria-label="toggle password visibility"
                                                            edge="end"
                                                            onClick={() => this.handleClickShowPassword(showPassword)}
                                                        >
                                                            {showPassword ? <VisibilityOff /> : <Visibility />}
                                                        </IconButton>
                                                        {messageText && <ErrorOutlineIcon className="errorIcon" />}
                                                    </InputAdornment>
                                                ),
                                            }}
                                        />
                                        {warning && <Link underline="hover"> Caps Lock is ON! </Link>}
                                        <Button
                                            className="loginButton"
                                            label=""
                                            type="submit"
                                            data-testid="submit"
                                            disabled={this.isDisabled()}
                                        >
                                            Log in
                                        </Button>
                                        <Spinner
                                            className="formfield form-spinner"
                                            label=""
                                            isLoading={isFetching}
                                            css={{
                                                position: 'relative',
                                                top: '70px',
                                                marginLeft: '-64px',
                                            }}
                                        />
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
