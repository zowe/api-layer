/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { useState } from 'react';
import {
    IconButton,
    InputAdornment,
    Typography,
    Button,
    CssBaseline,
    TextField,
    Link,
    Card,
    CardContent,
    CardActions,
} from '@material-ui/core';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import WarningIcon from '@material-ui/icons/Warning';
import ErrorOutlineIcon from '@material-ui/icons/ErrorOutline';
import Spinner from '../Spinner/Spinner';
import './Login.css';

const Login = (props) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [newPassword, setNewPassword] = useState(null);
    const [repeatNewPassword, setRepeatNewPassword] = useState(null);
    const [errorMessage] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [warning, setWarning] = useState(false);

    const { returnToLogin, login, authentication, isFetching, validateInput } = props;

    /**
     * Detect caps lock being on when typing.
     * @param keyEvent On key down event.
     */
    const onKeyDown = (keyEvent) => {
        setWarning(false);
        if (keyEvent.keyCode === 20 || keyEvent.keyCode === 16) {
            setWarning(true);
        } else {
            setWarning(false);
        }
    };

    /**
     * Detect caps lock being off when typing.
     * @param keyEvent On key up event.
     */
    const onKeyUp = (keyEvent) => {
        if (keyEvent.keyCode === 20 || keyEvent.keyCode === 16) {
            setWarning(false);
        } else {
            setWarning(true);
        }
    };

    function handleClickShowPassword() {
        setShowPassword(!showPassword);
    }

    const handleError = (auth) => {
        const { error, expired } = auth;
        let messageText;
        let invalidNewPassword;
        let isSuspended;
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
                (x) => x.messageKey != null && x.messageKey === error.messageNumber
            );
            invalidNewPassword = error.messageNumber === 'ZWEAT604E' || error.messageNumber === 'ZWEAT413E';
            isSuspended = error.messageNumber === 'ZWEAT414E';
            if (filter.length !== 0) {
                if (filter[0].messageKey === 'ZWEAS120E') {
                    messageText = `${filter[0].messageText}`;
                } else {
                    messageText = `(${error.messageNumber}) ${filter[0].messageText}`;
                }
            }
            if (invalidNewPassword || isSuspended) {
                messageText = `${filter[0].messageText}`;
            }
        } else if (error.status === 401 && authentication.sessionOn) {
            messageText = `(${errorMessages.messages[0].messageKey}) ${errorMessages.messages[0].messageText}`;
            authentication.onCompleteHandling();
        } else if (error.status === 500) {
            messageText = `(${errorMessages.messages[1].messageKey}) ${errorMessages.messages[1].messageText}`;
        }
        return { messageText, expired, invalidNewPassword, isSuspended };
    };

    function handleChange(target, setStateFunction) {
        setStateFunction(target.value);
        if (target.name === 'repeatNewPassword') {
            validateInput({ newPassword, repeatNewPassword: target.value });
        }
    }

    function backToLogin() {
        setNewPassword(null);
        setRepeatNewPassword(null);
        returnToLogin();
    }

    function handleSubmit(e) {
        e.preventDefault();

        if (username && password && newPassword) {
            login({ username, password, newPassword });
        } else if (username && password) {
            login({ username, password });
        }
    }

    let error = { messageText: null, expired: false, invalidNewPassword: true };
    if (
        authentication !== undefined &&
        authentication !== null &&
        authentication.error !== undefined &&
        authentication.error !== null
    ) {
        error = handleError(authentication);
        if (error.isSuspended) {
            return (
                <div className="login-object">
                    <div className="susp-card">
                        <Card variant="outlined">
                            <CardContent className="cardTitle">
                                <Typography sx={{ fontSize: 14 }} color="text.secondary" gutterBottom>
                                    <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                    <b>{error.messageText}</b>
                                </Typography>
                            </CardContent>
                            <CardContent>
                                <Typography variant="body2">
                                    <b>{username}</b> account has been suspended.
                                </Typography>
                                <br />
                                <Typography variant="body2">
                                    Contact your security administrator to unsuspend your account.
                                </Typography>
                            </CardContent>
                            <CardActions>
                                <Button
                                    variant="outlined"
                                    className="backBtn"
                                    color="primary"
                                    label=""
                                    size="medium"
                                    style={{ border: 'none' }}
                                    onClick={backToLogin}
                                    data-testid="suspendedBackToLogin"
                                >
                                    RETURN TO LOGIN
                                </Button>
                            </CardActions>
                        </Card>
                    </div>
                </div>
            );
        }
    } else if (errorMessage) {
        error.messageText = errorMessage;
    }

    return (
        <div className="login-object">
            <div className="susp-card">
                <div className="w-form">
                    <form
                        id="login-form"
                        name="login-form"
                        data-testid="login-form"
                        data-name="Login Form"
                        className="form"
                        onSubmit={handleSubmit}
                    >
                        <CssBaseline />
                        <div className="text-block-4">API Catalog</div>
                        <br />
                        {error.messageText !== undefined && error.messageText !== null && (
                            <div id="error-message">
                                <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                {error.messageText}
                            </div>
                        )}
                        {!error.expired && (
                            <div>
                                <Typography className="login-typo" variant="subtitle1" gutterBottom component="div">
                                    Login
                                </Typography>
                                <Typography variant="subtitle2" gutterBottom component="div">
                                    Please enter your mainframe username and password to access this resource
                                </Typography>
                                <TextField
                                    label="Username"
                                    data-testid="username"
                                    className="formfield"
                                    variant="outlined"
                                    required
                                    error={!!error.messageText}
                                    fullWidth
                                    id="username"
                                    name="username"
                                    value={username}
                                    onChange={(t) => handleChange(t.target, setUsername)}
                                    autoComplete="on"
                                    autoFocus
                                />
                                <br />
                                <TextField
                                    id="password"
                                    htmlFor="outlined-adornment-password"
                                    label="Password"
                                    data-testid="password"
                                    className="formfield"
                                    variant="outlined"
                                    required
                                    error={!!error.messageText}
                                    fullWidth
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onKeyDown={onKeyDown}
                                    onKeyUp={onKeyUp}
                                    onChange={(t) => handleChange(t.target, setPassword)}
                                    caption="Default: password"
                                    autoComplete="on"
                                    InputProps={{
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                {error.messageText && <ErrorOutlineIcon className="errorIcon" />}
                                                <IconButton
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                                {warning && (
                                    <Link id="capslock" data-testid="caps-lock-on" underline="hover">
                                        Caps Lock is ON!
                                    </Link>
                                )}
                                <div className="login-btns" id="loginButton">
                                    <Button
                                        variant="contained"
                                        color="primary"
                                        label=""
                                        size="medium"
                                        style={{ border: 'none' }}
                                        type="submit"
                                        data-testid="submit"
                                        disabled={isFetching}
                                    >
                                        LOG IN
                                    </Button>
                                </div>
                            </div>
                        )}
                        {error.expired && (
                            <div>
                                <TextField
                                    id="newPassword"
                                    htmlFor="outlined-adornment-password"
                                    label="New Password"
                                    data-testid="newPassword"
                                    className="formfield"
                                    variant="outlined"
                                    required
                                    error={error.invalidNewPassword}
                                    fullWidth
                                    name="newPassword"
                                    type={showPassword ? 'text' : 'password'}
                                    value={newPassword}
                                    onKeyDown={onKeyDown}
                                    onChange={(t) => handleChange(t.target, setNewPassword)}
                                    caption="Default: new password"
                                    autoComplete="on"
                                    InputProps={{
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                {error.messageText && <ErrorOutlineIcon className="errorIcon" />}
                                                <IconButton
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                                <br />
                                <TextField
                                    id="repeatNewPassword"
                                    htmlFor="outlined-adornment-password"
                                    label="Repeat New Password"
                                    data-testid="repeatNewPassword"
                                    className="formfield"
                                    variant="outlined"
                                    required
                                    error={error.invalidNewPassword}
                                    fullWidth
                                    name="repeatNewPassword"
                                    type={showPassword ? 'text' : 'password'}
                                    value={repeatNewPassword}
                                    onKeyDown={onKeyDown}
                                    onChange={(t) => handleChange(t.target, setRepeatNewPassword)}
                                    caption="Default: Repeat new password"
                                    autoComplete="on"
                                    InputProps={{
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                {error.messageText && <ErrorOutlineIcon className="errorIcon" />}
                                                <IconButton
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                                <div className="login-btns">
                                    <Button
                                        variant="outlined"
                                        className="backBtn"
                                        color="primary"
                                        label=""
                                        size="medium"
                                        style={{ border: 'none' }}
                                        onClick={backToLogin}
                                        data-testid="backToLogin"
                                        disabled={isFetching}
                                    >
                                        BACK
                                    </Button>
                                    <Button
                                        variant="contained"
                                        className="updateBtn"
                                        color="primary"
                                        label=""
                                        size="medium"
                                        style={{ border: 'none' }}
                                        type="submit"
                                        data-testid="submitChange"
                                        disabled={!repeatNewPassword || error.invalidNewPassword}
                                    >
                                        CHANGE PASSWORD
                                    </Button>
                                </div>
                            </div>
                        )}
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
    );
};

export default Login;
