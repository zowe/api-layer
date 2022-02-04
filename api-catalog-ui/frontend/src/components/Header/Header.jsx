/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component } from 'react';
import { IconButton, Link, Typography, Tooltip } from '@material-ui/core';
import PowerSettingsNewIcon from '@material-ui/icons/PowerSettingsNew';
import productImage from '../../assets/images/api-catalog-logo.png';
import './Header.css';

export default class Header extends Component {
    handleLogout = () => {
        const { logout } = this.props;
        logout();
    };

    render() {
        const iconLogout = <PowerSettingsNewIcon id="logoutIcon" style={{ color: 'white' }} />;
        const dashboard = 'ui/v1/apicatalog/#/dashboard';
        return (
            <div className="header">
                <div className="product-name">
                    <Link data-testid="link" href={dashboard}>
                        <div className="app-icon-container">
                            <img id="logo" alt="API Catalog Product Name" src={productImage} />
                        </div>
                    </Link>
                    <Link href={dashboard}>
                        <Typography variant="subtitle2">API Catalog</Typography>
                    </Link>
                </div>
                <div className="right-icons">
                    <div className="logout-container">
                        <Tooltip title="Logout">
                            <IconButton id="logout-button" data-testid="logout" onClick={this.handleLogout}>
                                {iconLogout}
                            </IconButton>
                        </Tooltip>
                    </div>
                </div>
            </div>
        );
    }
}
