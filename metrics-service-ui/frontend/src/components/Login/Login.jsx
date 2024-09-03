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
import { Button, TextField, Typography } from '@material-ui/core';
import { withStyles, makeStyles, useTheme } from '@material-ui/core/styles';

import './LoginWebflow.css';
import LoginBackground from '../../assets/images/login_background.jpg';
import Spinner from '../Spinner/Spinner';
import MetricsIconButton from '../Icons/MetricsIconButton';
import ErrorComponent from '../Error/ErrorComponent';

const useStyles = makeStyles((theme) => ({
    root: {
        overflow: 'hidden',
        'background-image': `url(${LoginBackground})`,
        'background-size': 'cover',
        'background-repeat': 'no-repeat',
        display: 'flex',
        flex: 'auto',
        height: '100%',
        'padding-bottom': 0,
        '@media only screen and (max-width: 1315px)': {
            picture: {
                display: 'none',
            },
        },
    },
    formContainer: {
        height: '100vh',
        width: '440px',
        'background-color': theme.palette.background.main,
    },
    form: {
        display: 'flex',
        'flex-direction': 'column',
    },
}));

const MetricsServiceTitle = withStyles((theme) => ({
    h5: {
        color: theme.palette.primary.main,
        fontWeight: 'bold',
        marginTop: 90,
        marginBottom: 90,
        marginLeft: 20,
    },
}))(Typography);

const FormField = withStyles(() => ({
    root: {
        margin: 40,
        marginTop: 0,
    },
}))(TextField);

const SubmitButton = withStyles((theme) => ({
    root: {
        margin: 40,
        padding: 15,
        '&:hover': {
            backgroundColor: theme.palette.primary.light,
        },
    },
}))(Button);

const LoginError = withStyles(() => ({
    root: {
        marginLeft: 20,
    },
}))(ErrorComponent);

function unexpectedError(error) {
    return (
        error.messageNumber !== undefined &&
        error.messageNumber !== null &&
        error.messageType !== undefined &&
        error.messageType !== null
    );
}

function Login(props) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { authentication, isFetching, errorText } = props;

    const handleError = (error) => {
        let errorMessageText;

        // eslint-disable-next-line global-require
        const errorMessages = require('../../error-messages.json');
        if (unexpectedError(error)) {
            errorMessageText = `Unexpected error, please try again later (${error.messageNumber})`;
            const filter = errorMessages.messages.filter(
                (x) => x.messageKey != null && x.messageKey === error.messageNumber
            );
            if (filter.length !== 0) errorMessageText = `(${error.messageNumber}) ${filter[0].messageText}`;
        } else if (error.status === 401 && authentication.sessionOn) {
            errorMessageText = `(${errorMessages.messages[0].messageKey}) ${errorMessages.messages[0].messageText}`;
            authentication.onCompleteHandling();
        } else if (error.status === 500) {
            errorMessageText = `(${errorMessages.messages[1].messageKey}) ${errorMessages.messages[1].messageText}`;
        }

        return errorMessageText;
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        const { login } = props;
        if (username && password) {
            login({ username, password });
        }
    };

    const messageText =
        authentication !== undefined &&
        authentication !== null &&
        authentication.error !== undefined &&
        authentication.error !== null
            ? handleError(authentication.error)
            : errorText;

    const theme = useTheme();
    const classes = useStyles();

    return (
        <div className={classes.root}>
            <div className={classes.formContainer}>
                <MetricsServiceTitle variant="h5" align="left">
                    <MetricsIconButton color={theme.palette.primary.main} />
                    Metrics Service
                </MetricsServiceTitle>
                <form className={classes.form} onSubmit={handleSubmit}>
                    <FormField
                        label="Username"
                        name="username"
                        value={username}
                        id="username"
                        type="text"
                        onChange={(t) => setUsername(t.target.value)}
                    />
                    <FormField
                        label="Password"
                        name="password"
                        value={password}
                        id="password"
                        type="password"
                        onChange={(t) => setPassword(t.target.value)}
                    />
                    <SubmitButton
                        type="submit"
                        id="submit"
                        variant="contained"
                        color="primary"
                        size="medium"
                        disabled={isFetching}
                        onClick={handleSubmit}
                    >
                        Sign in
                    </SubmitButton>
                    <Spinner
                        className="formfield form-spinner"
                        isLoading={isFetching}
                        css={{
                            position: 'relative',
                            top: '70px',
                        }}
                    />
                    {messageText !== undefined && messageText !== null && (
                        <LoginError id="errormessage" text={messageText} />
                    )}
                </form>
            </div>
        </div>
    );
}

export default Login;
