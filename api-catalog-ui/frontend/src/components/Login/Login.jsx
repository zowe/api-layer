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
    Card,
    CardContent,
    CardActions,
    Grid,
    Box,
    FormControl,
    OutlinedInput,
    InputLabel,
    FormHelperText,
} from '@material-ui/core';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import WarningIcon from '@material-ui/icons/Warning';
import Spinner from '../Spinner/Spinner';
import './Login.css';

function Login(props) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [repeatNewPassword, setRepeatNewPassword] = useState('');
    const [errorMessage] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [warning, setWarning] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    const { returnToLogin, login, authentication, isFetching, validateInput } = props;
    const enterNewPassMsg = 'Enter a new password for account';
    const invalidPassMsg = 'The specified username or password is invalid.';

    /**
     * Detect caps lock being off when typing.
     * @param keyEvent On key up event.
     */
    const onKeyEvent = (keyEvent) => {
        if (keyEvent.getModifierState('CapsLock')) {
            setWarning(true);
        } else {
            setWarning(false);
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
        let invalidCredentials;
        // eslint-disable-next-line global-require
        const errorMessages = require('../../error-messages.json');
        if (error?.messageNumber && error?.messageType && error?.messageContent) {
            messageText = `(${error.messageNumber}) ${error.messageContent}`;
            const filter = errorMessages.messages.filter(
                (x) => x.messageKey != null && x.messageKey === error.messageNumber
            );
            invalidNewPassword = error.messageNumber === 'ZWEAT604E' || error.messageNumber === 'ZWEAT413E';
            isSuspended = error.messageNumber === 'ZWEAT414E';
            invalidCredentials = filter[0]?.messageKey === 'ZWEAS120E';
            if (filter.length !== 0) {
                if (invalidCredentials || filter[0].messageKey === 'ZWEAT412E') {
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
        return { messageText, expired, invalidNewPassword, isSuspended, invalidCredentials };
    };

    function handleChange(target, setStateFunction) {
        setStateFunction(target.value);
        if (target.name === 'repeatNewPassword') {
            validateInput({ newPassword, repeatNewPassword: target.value });
            setSubmitted(false);
        }
    }

    function backToLogin() {
        setNewPassword(null);
        setRepeatNewPassword(null);
        returnToLogin();
    }

    function handleSubmit(e) {
        e.preventDefault();
        localStorage.setItem('username', username);
        if (username && password && newPassword) {
            login({ username, password, newPassword });
        } else if (username && password) {
            login({ username, password });
        }
        setSubmitted(true);
    }

    let errorData = { messageText: null, expired: false, invalidNewPassword: true, invalidCredentials: false };
    if (
        authentication !== undefined &&
        authentication !== null &&
        authentication.error !== undefined &&
        authentication.error !== null
    ) {
        errorData = handleError(authentication);
        if (errorData.isSuspended) {
            return (
                <div className="login-object">
                    <div className="susp-card">
                        <Card variant="outlined">
                            <CardContent className="cardTitle">
                                <div className="susp-acc">
                                    <WarningIcon style={{ color: '#de1b1b' }} fontSize="medium" />
                                    <Typography className="susp-msg" variant="h6">
                                        {errorData.messageText}
                                    </Typography>
                                </div>
                            </CardContent>
                            <CardContent>
                                <Box sx={{ width: 300 }}>
                                    <Typography variant="body2">
                                        <b>{username}</b> account has been suspended.
                                    </Typography>
                                    <br />
                                    <Typography variant="body2">
                                        Contact your security administrator to unsuspend your account.
                                    </Typography>
                                </Box>
                            </CardContent>
                            <CardActions>
                                <Grid container justifyContent="flex-end">
                                    <Button
                                        variant="outlined"
                                        className="backBtn"
                                        color="primary"
                                        label=""
                                        size="medium"
                                        style={{ border: 'none' }}
                                        onClick={() => backToLogin()}
                                        data-testid="suspendedBackToLogin"
                                    >
                                        RETURN TO LOGIN
                                    </Button>
                                </Grid>
                            </CardActions>
                        </Card>
                    </div>
                </div>
            );
        }
    } else if (errorMessage) {
        errorData.messageText = errorMessage;
    }

    return (
        <div className="login-object">
            <Spinner label="" isLoading={isFetching} />
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
                        {errorData.messageText !== undefined &&
                            errorData.messageText !== null &&
                            !authentication.expiredWarning && (
                                <div id="error-message">
                                    <div id="warn-first-line">
                                        <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                        <Typography className="susp-msg" variant="body1">
                                            {errorData.messageText}
                                        </Typography>
                                    </div>
                                    <Typography variant="body2">
                                        {errorData.expired && (
                                            <p>
                                                {enterNewPassMsg} <b>{username}</b>
                                            </p>
                                        )}
                                        {errorData.invalidCredentials && <p>{invalidPassMsg}</p>}
                                    </Typography>
                                </div>
                            )}
                        {errorData.messageText !== undefined &&
                            errorData.messageText !== null &&
                            errorData.expired &&
                            authentication.expiredWarning && (
                                <div id="warn-message">
                                    <div id="warn-first-line">
                                        <WarningIcon style={{ color: '#de1b1b' }} size="2rem" />
                                        <Typography className="susp-msg" variant="body1">
                                            Password Expired
                                        </Typography>
                                    </div>
                                    <Typography variant="body2">
                                        Your Password for account <b>{username}</b> has expired. Enter a new password.
                                    </Typography>
                                </div>
                            )}
                        {!errorData.expired && (
                            <div>
                                <Typography className="login-typo" variant="subtitle1" gutterBottom component="div">
                                    Login
                                </Typography>
                                <Typography variant="subtitle2" gutterBottom component="div">
                                    Please enter your mainframe username and password
                                </Typography>
                                <TextField
                                    label="Username"
                                    className="formfield"
                                    variant="outlined"
                                    data-testid="user"
                                    required
                                    error={!!errorData.messageText}
                                    inputProps={{ 'data-testid': 'username' }}
                                    fullWidth
                                    id="username"
                                    name="username"
                                    value={username}
                                    onChange={(t) => handleChange(t.target, setUsername)}
                                    autoComplete="on"
                                    autoFocus
                                />
                                <br />
                                <FormControl required fullWidth>
                                    <InputLabel variant="outlined">Password</InputLabel>
                                    <OutlinedInput
                                        id="component-outlined"
                                        className="formfield"
                                        data-testid="pass"
                                        aria-describedby="my-helper-text"
                                        error={!!errorData.messageText}
                                        name="password"
                                        inputProps={{ 'data-testid': 'password' }}
                                        type={showPassword ? 'text' : 'password'}
                                        value={password}
                                        onKeyDown={onKeyEvent}
                                        onKeyUp={onKeyEvent}
                                        onChange={(t) => handleChange(t.target, setPassword)}
                                        autoComplete="on"
                                        label="Password"
                                        endAdornment={
                                            <InputAdornment position="end">
                                                {errorData.messageText && <WarningIcon className="errorIcon" />}
                                                <IconButton
                                                    className="visibility-icon"
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        }
                                    />
                                    {warning && (
                                        <FormHelperText
                                            error
                                            id="capslock"
                                            data-testid="caps-lock-on"
                                            underline="hover"
                                        >
                                            Caps Lock is ON!
                                        </FormHelperText>
                                    )}
                                </FormControl>
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
                        {errorData.expired && (
                            <div>
                                <FormControl required fullWidth>
                                    <InputLabel variant="outlined">New Password</InputLabel>
                                    <OutlinedInput
                                        id="newPass"
                                        error={errorData.invalidNewPassword}
                                        name="newPassword"
                                        data-testid="newPassword"
                                        type={showPassword ? 'text' : 'password'}
                                        value={newPassword}
                                        onKeyDown={onKeyEvent}
                                        onKeyUp={onKeyEvent}
                                        onChange={(t) => handleChange(t.target, setNewPassword)}
                                        autoComplete="on"
                                        label="New Password"
                                        endAdornment={
                                            <InputAdornment position="end">
                                                {errorData.invalidNewPassword && <WarningIcon className="errorIcon" />}
                                                <IconButton
                                                    className="visibility-icon"
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        }
                                    />
                                </FormControl>
                                <br />
                                <FormControl required fullWidth>
                                    <InputLabel variant="outlined">Repeat New Password</InputLabel>
                                    <OutlinedInput
                                        id="component-outlined"
                                        aria-describedby="my-helper-text"
                                        data-testid="repeatNewPassword"
                                        error={errorData.invalidNewPassword}
                                        name="repeatNewPassword"
                                        type={showPassword ? 'text' : 'password'}
                                        value={repeatNewPassword}
                                        onKeyDown={onKeyEvent}
                                        onKeyUp={onKeyEvent}
                                        onChange={(t) => handleChange(t.target, setRepeatNewPassword)}
                                        caption="Default: Repeat new password"
                                        autoComplete="on"
                                        label="Repeat New Password"
                                        endAdornment={
                                            <InputAdornment position="end">
                                                {errorData.invalidNewPassword && <WarningIcon className="errorIcon" />}
                                                <IconButton
                                                    className="visibility-icon"
                                                    aria-label="toggle password visibility"
                                                    edge="end"
                                                    onClick={() => handleClickShowPassword()}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        }
                                    />
                                    {repeatNewPassword && !authentication.matches && (
                                        <FormHelperText error id="my-helper-text">
                                            Passwords do not match.
                                        </FormHelperText>
                                    )}
                                    {warning && (
                                        <FormHelperText
                                            error
                                            id="capslock"
                                            data-testid="caps-lock-on"
                                            underline="hover"
                                        >
                                            Caps Lock is ON!
                                        </FormHelperText>
                                    )}
                                </FormControl>
                                <div className="login-btns">
                                    <Button
                                        variant="outlined"
                                        className="backBtn"
                                        color="primary"
                                        label=""
                                        size="medium"
                                        style={{ border: 'none' }}
                                        onClick={() => backToLogin()}
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
                                        disabled={!repeatNewPassword || !authentication.matches || submitted}
                                    >
                                        CHANGE PASSWORD
                                    </Button>
                                </div>
                            </div>
                        )}
                    </form>
                </div>
            </div>
        </div>
    );
}

export default Login;
