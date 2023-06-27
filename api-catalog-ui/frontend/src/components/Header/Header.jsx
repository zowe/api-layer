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
import { Button, Link, Typography, Menu, MenuItem, Divider, makeStyles, styled } from '@material-ui/core';
import PersonIcon from '@material-ui/icons/Person';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import productImage from '../../assets/images/api-catalog-logo.png';
import zoweDocsImage from '../../assets/images/zowe-docs.png';
import zoweAuthImage from '../../assets/images/zowe-auth.png';

const useStyles = makeStyles({
    root: {
        '&:hover': {
            backgroundColor: 'rgb(86, 145, 240)',
        },
    },
});

const StyledMenu = styled((props) => (
    <Menu
        elevation={0}
        anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
        }}
        transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
        }}
        {...props}
    />
))(({ theme }) => ({
    '& .MuiPaper-root': {
        borderRadius: 6,
        marginTop: theme.spacing(2),
        minWidth: 150,
        '& .MuiMenu-list': {
            padding: '0',
        },
        '& .MuiMenuItem-root': {
            '& .MuiSvgIcon-root': {
                fontSize: 10,
                color: theme.palette.text.secondary,
            },
        },
    },
}));
function Header(props) {
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
    const dashboard = '#/dashboard';
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
                {process.env.REACT_APP_API_PORTAL !== undefined && process.env.REACT_APP_API_PORTAL === 'true' && (
                    <div id="zowe-links">
                        <Link rel="noopener noreferrer" target="_blank" href="https://docs.zowe.org">
                            <img id="doc" alt="Zowe docs" src={zoweDocsImage} />
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://docs.zowe.org/stable/extend/extend-apiml/authentication-for-apiml-services/#authentication-endpoints"
                        >
                            <img id="auth" alt="Zowe authentication" src={zoweAuthImage} />
                        </Link>
                    </div>
                )}
                {process.env.REACT_APP_API_PORTAL !== undefined && process.env.REACT_APP_API_PORTAL === 'false' && (
                    <div className="logout-container">
                        <Button
                            className={classes.root}
                            data-testid="logout-menu"
                            aria-controls={open ? 'basic-menu' : undefined}
                            aria-expanded={open ? 'true' : undefined}
                            aria-haspopup="true"
                            aria-label="more"
                            onClick={openMenu}
                            endIcon={<KeyboardArrowDownIcon id="down-arrow" />}
                        >
                            {s}
                        </Button>
                        <StyledMenu
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
                                <Typography variant="subtitle2" gutterBottom component="div" id="user-info-text">
                                    Logged in as <strong>{username}</strong>
                                </Typography>
                                <Divider />
                                <MenuItem id="logout-button" data-testid="logout" onClick={handleLogout}>
                                    Log out
                                </MenuItem>
                            </div>
                        </StyledMenu>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Header;
