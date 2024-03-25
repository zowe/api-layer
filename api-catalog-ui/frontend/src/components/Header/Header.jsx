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
import productImage from '../../assets/images/broadcom.svg';
import MenuImage from '../../assets/images/hamburger.svg';
import customDoc from '../../assets/images/ExternalLink.svg';
import { isAPIPortal, openMobileMenu, closeMobileMenu } from '../../utils/utilFunctions';
import MenuCloseImage from '../../assets/images/xmark.svg';

const loadFeedbackButton = () => {
    if (isAPIPortal()) {
        return import('../FeedbackButton/FeedbackButton');
    }
    return Promise.resolve(null);
};

const FeedbackButton = React.lazy(loadFeedbackButton);

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
    const homepage = '#/homepage';
    const username = localStorage.getItem('username');
    return (
        <div className="header">
            {isAPIPortal() && (
                <div className="mobile-view mobile-menu-trigger-ctn">
                    <Button className="mobile-menu-trigger-btn icon-btn" aria-label="menu" onClick={openMobileMenu}>
                        <img alt="Menu" src={MenuImage} className="mobile-menu-trigger" />
                    </Button>
                </div>
            )}

            <div className="product-name">
                <Link data-testid="link" href={homepage}>
                    <div className="app-icon-container">
                        <img id="logo" alt="API Catalog" src={productImage} />
                        {isAPIPortal() && <span className="tooltiptext">Mainframe Developer Homepage</span>}
                    </div>
                </Link>
            </div>

            {isAPIPortal() && (
                <div className="mobile-view mobile-menu-close-ctn">
                    <Button
                        className="mobile-menu-close-btn icon-btn mobile-view"
                        aria-label="close-menu"
                        onClick={closeMobileMenu}
                    >
                        <img alt="Menu" src={MenuCloseImage} className="mobile-menu-close" />
                    </Button>
                </div>
            )}

            {isAPIPortal() && <h3 className="mobile-product-title title1 mobile-view">API Catalog</h3>}
            {isAPIPortal() && <h4 className="title2 mobile-view">Useful Links</h4>}

            <div className="right-icons">
                {isAPIPortal() && (
                    <>
                        <Link href={dashboard} className="dashboard-develop-link">
                            Design Using APIs
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://docs.zowe.org/stable/extend/extend-apiml/authentication-for-apiml-services/#authentication-endpoints"
                        >
                            Zowe<sup className="registered">&reg;</sup> Authentication
                            <img id="img-internal-link" alt="Internal doc" src={customDoc} className="ext" />
                        </Link>
                        <Link rel="noopener noreferrer" target="_blank" href="https://docs.zowe.org">
                            Zowe<sup className="registered">&reg;</sup> Docs
                            <img id="img-internal-link" alt="Internal doc" src={customDoc} className="ext" />
                        </Link>
                    </>
                )}
                <Link
                    data-testid="internal-link"
                    rel="noopener noreferrer"
                    target="_blank"
                    href="https://techdocs.broadcom.com/us/en/ca-mainframe-software.html"
                >
                    Broadcom TechDocs
                    <img id="img-internal-link" alt="Internal doc" src={customDoc} className="ext" />
                </Link>

                {!isAPIPortal() && (
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

            {isAPIPortal() && (
                <div className="mobile-view feedback-ctn">
                    <h2 className="title2 mobile-view">Get in Touch</h2>
                    {isAPIPortal() && <FeedbackButton noFloat mobile />}
                </div>
            )}
        </div>
    );
}

export default Header;
