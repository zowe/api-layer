/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { Component } from 'react';
import { IconButton, Typography } from '@material-ui/core';
import HeaderContainer from '../Header/HeaderContainer';
import Footer from '../Footer/Footer';
import './PageNotFound.css';

export default class PageNotFound extends Component {
    handleGoToHome = () => {
        const { history } = this.props;
        history.push('/dashboard');
    };

    render() {
        const backButtonText = '<< Back to Homepage';
        return (
            <div className="page-not-found-container">
                <HeaderContainer />
                <br />
                <div>
                    <div className="api-heading" />
                    <h1 id="primary-label" className="api-heading">
                        {' '}
                        404 - Page Not Found{' '}
                    </h1>
                </div>
                <Typography id="secondary-label" variant="h5">
                    There's nothing way out here. Best to go back.
                </Typography>
                <div>
                    <IconButton
                        className="button-cta"
                        id="go-back-button"
                        data-testid="go-home-button"
                        onClick={this.handleGoToHome}
                        size="medium"
                    >
                        {backButtonText}
                    </IconButton>
                </div>
                <Footer />
            </div>
        );
    }
}
