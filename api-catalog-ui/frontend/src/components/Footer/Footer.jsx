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
import { Container, Link, Typography } from '@material-ui/core';
import linkedInImg from '../../assets/images/linkedin-red.png';
import twitterImg from '../../assets/images/twitter-square.png';
import youtubeImg from '../../assets/images/youtube-square.png';
import youtubeImg2 from '../../assets/images/youtube2.png';
import mailImg from '../../assets/images/square-envelope.png';
import productImage from '../../assets/images/broadcom.svg';
import FeedbackForm from '../FeedbackForm/FeedbackForm';

// TODO sitemap link is currently empty. Note: The broadcom.svg must be placed in ../../assets/images api-layer folder as part of the automation
export default class Footer extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isDialogOpen: false,
        };
        this.handleDialogClose = this.handleDialogClose.bind(this);
        this.handleDialogOpen = this.handleDialogOpen.bind(this);
    }

    handleDialogOpen = () => {
        this.setState({
            isDialogOpen: true,
        });
    };

    handleDialogClose = () => {
        this.setState({ isDialogOpen: false });
    };

    render() {
        const { isDialogOpen } = this.state;
        const dashboard = '#/dashboard';
        return (
            <footer id="pageFooter">
                <div id="bottom-info-div" style={{ marginBottom: '30px', marginTop: '60px' }}>
                    {isDialogOpen && <FeedbackForm handleDialog={this.handleDialogClose} isDialogOpen={isDialogOpen} />}
                    <Container>
                        <strong>Broadcom Mainframe Software</strong>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/solutions/mainframe"
                            className="links"
                        >
                            Mainframe Software Solutions
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/products/mainframe"
                            className="links"
                        >
                            Mainframe Software Products
                        </Link>
                    </Container>
                    <Container>
                        <strong>News & Information</strong>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/news"
                            className="links"
                        >
                            Broadcom in the News
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/about-us"
                            className="links"
                        >
                            About Broadcom
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.mainframe.broadcom.com/beyondcode"
                            className="links"
                        >
                            Customer Programs
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://community.broadcom.com/mainframesoftware/home"
                            className="links"
                        >
                            Education & Training
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://support.broadcom.com/"
                            className="links"
                        >
                            Support Portal
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/support/services/mainframe"
                            className="links"
                        >
                            Professional Services
                        </Link>
                    </Container>
                    <Container>
                        <strong>Contact Us</strong>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.linkedin.com/showcase/broadcom-mainframe-software/"
                            className="footer-links"
                            style={{ paddingRight: '5px' }}
                        >
                            <img id="linkedin" alt="linkedin" src={linkedInImg} />
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://twitter.com/broadcommsd"
                            className="footer-links"
                            style={{ paddingRight: '5px' }}
                        >
                            <img id="twitter" alt="twitter" src={twitterImg} />
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.youtube.com/user/MyCACommunities"
                            className="footer-links"
                            style={{ paddingRight: '5px' }}
                        >
                            <img id="youtube" alt="youtube" src={youtubeImg} />
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://broadcom-mainframe-software.hubs.vidyard.com"
                            className="footer-links"
                            style={{ paddingRight: '5px' }}
                        >
                            <img id="flix" style={{ height: 'auto', width: '29px' }} alt="flix" src={youtubeImg2} />
                        </Link>
                        <Link className="footer-links" onClick={() => this.handleDialogOpen()}>
                            <img id="email" alt="email" src={mailImg} />
                        </Link>
                    </Container>
                </div>
                <div>
                    <div className="left">
                        <Typography
                            style={{ marginLeft: '130px', float: 'left' }}
                            id="footer-message"
                            variant="subtitle2"
                        >
                            <Link href={dashboard}>
                                <img
                                    style={{
                                        width: '105px',
                                        height: '21px',
                                        marginRight: '42px',
                                        marginBottom: '-6px',
                                    }}
                                    id="footer-logo"
                                    alt="API Catalog"
                                    src={productImage}
                                />
                            </Link>
                            Copyright © 2005-2023 Broadcom. All Rights Reserved. The term “Broadcom” refers to Broadcom
                            Inc. and/or its subsidiaries.
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.broadcom.com/company/legal/privacy-policy"
                                style={{ marginLeft: '50px', fontSize: '11px' }}
                            >
                                {' '}
                                Privacy Policy |
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.broadcom.com/company/legal/privacy/cookie-policy"
                                style={{ fontSize: '11px' }}
                            >
                                {' '}
                                Cookies Policy |
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.broadcom.com/company/legal/privacy/data-transfers"
                                style={{ fontSize: '11px' }}
                            >
                                {' '}
                                Data Processing and Data Transfers |
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.broadcom.com/company/citizenship/supplier-responsibility#supply"
                                style={{ fontSize: '11px' }}
                            >
                                {' '}
                                Supplier Responsibility |
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.broadcom.com/company/legal/terms-of-use/"
                                style={{ fontSize: '11px' }}
                            >
                                {' '}
                                Terms of Use |
                            </Link>
                            <Link rel="noopener noreferrer" target="_blank" href="" style={{ fontSize: '11px' }}>
                                {' '}
                                Sitemap
                            </Link>
                        </Typography>
                        <p />
                        <br />
                        <br />
                        <br />
                        <br />
                    </div>
                </div>
            </footer>
        );
    }
}
