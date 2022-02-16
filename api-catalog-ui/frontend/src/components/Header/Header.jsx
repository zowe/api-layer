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
import { Button, Link, Typography, Menu, MenuItem, makeStyles } from '@material-ui/core';
import PersonIcon from '@material-ui/icons/Person';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import productImage from '../../assets/images/api-catalog-logo.png';
import './Header.css';

const useStyles = makeStyles({
    root: {
        '&:hover': {
            backgroundColor: 'rgba(16,155,255,0.94)',
        },
    },
});
const Header = (props) => {
    const [open, setOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState(null);
    const { logout } = props;
    const classes = useStyles();
    const handleLogout = () => {
        logout();
    };

    const closeMenu = () => {
        setOpen(false);
    };

    const openMenu = (event) => {
        setOpen(true);
        setAnchorEl(event.target);
    };

    const s = <PersonIcon id="profileIcon" style={{ color: 'white' }} />;
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
                        className={classes.root}
                        aria-controls={open ? 'basic-menu' : undefined}
                        aria-expanded={open ? 'true' : undefined}
                        aria-haspopup="true"
                        aria-label="more"
                        onClick={openMenu}
                        endIcon={<KeyboardArrowDownIcon id="down-arrow" />}
                    >
                        {s}
                    </Button>
                    <Menu
                        keepMounted
                        open={open}
                        onClose={closeMenu}
                        anchorEl={anchorEl}
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
                            <MenuItem id="logout-button" data-testid="logout" onClick={handleLogout}>
                                Sign out
                            </MenuItem>
                        </div>
                    </Menu>
                </div>
            </div>
        </div>
    );
};

export default Header;
