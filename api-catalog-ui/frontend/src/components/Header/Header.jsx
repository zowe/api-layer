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
import { Button, Link, Typography, Menu, MenuItem } from '@material-ui/core';
import PersonIcon from '@material-ui/icons/Person';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import productImage from '../../assets/images/api-catalog-logo.png';
import './Header.css';

export default class Header extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
            anchorEl: null,
        };
        this.handleLogout = this.handleLogout.bind(this);
        this.openMenu = this.openMenu.bind(this);
        this.handleLogout = this.handleLogout.bind(this);
        this.closeMenu = this.closeMenu.bind(this);
    }
    handleLogout = () => {
        const { logout } = this.props;
        logout();
    };

    closeMenu() {
        this.setState({ isOpen: false });
    }

    openMenu(event) {
        this.setState({ isOpen: true, anchorEl: event.target });
    }

    render() {
        const iconProfile = <PersonIcon id="profileIcon" style={{ color: 'white' }} />;
        const dashboard = 'ui/v1/apicatalog/#/dashboard';
        const username = localStorage.getItem('username');
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
                        <Button
                            aria-controls={this.state.isOpen ? 'basic-menu' : undefined}
                            aria-expanded={this.state.isOpen ? 'true' : undefined}
                            aria-haspopup="true"
                            aria-label="more"
                            onClick={this.openMenu}
                            endIcon={<KeyboardArrowDownIcon />}
                        >
                            {iconProfile}
                        </Button>
                        <Menu
                            keepMounted
                            open={this.state.isOpen}
                            onClose={this.closeMenu}
                            anchorEl={this.state.anchorEl}
                            getContentAnchorEl={null}
                            anchorOrigin={{
                                vertical: 'bottom',
                                horizontal: 'center',
                            }}
                            transformOrigin={{
                                vertical: 'top',
                                horizontal: 'center',
                            }}
                        >
                            <div id="profile-menu">
                                Signed in as <strong>{username}</strong>
                                <MenuItem id="logout-button" data-testid="logout" onClick={this.handleLogout}>
                                    Sign out
                                </MenuItem>
                            </div>
                        </Menu>
                    </div>
                </div>
            </div>
        );
    }
}
