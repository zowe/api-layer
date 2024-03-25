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
import subscribeImg from '../../assets/images/Subscribe.png';
import productImage from '../../assets/images/broadcom.svg';

export default class Footer extends Component {
    render() {
        const dashboard = '#/dashboard';
        return (
            <footer id="pageFooter">
                <div id="bottom-info-div">
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
                        <div>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.linkedin.com/showcase/broadcom-mainframe-software/"
                                className="footer-links"
                            >
                                <img id="linkedin" alt="linkedin" src={linkedInImg} />
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://twitter.com/broadcommsd"
                                className="footer-links"
                            >
                                <img id="twitter" alt="twitter" src={twitterImg} />
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://www.youtube.com/user/MyCACommunities"
                                className="footer-links"
                            >
                                <img id="youtube" alt="youtube" src={youtubeImg} />
                            </Link>
                            <Link
                                rel="noopener noreferrer"
                                target="_blank"
                                href="https://broadcom-mainframe-software.hubs.vidyard.com"
                                className="footer-links"
                            >
                                <img id="flix" style={{ height: 'auto', width: '29px' }} alt="flix" src={youtubeImg2} />
                            </Link>
                            <Link className="footer-links" href="/feedback">
                                <img id="email" alt="email" src={mailImg} />
                            </Link>
                        </div>
                        <div>
                            <Link className="footer-links">
                                <img id="subscribe" alt="subscribe" src={subscribeImg} />
                            </Link>
                        </div>
                    </Container>
                </div>
                <Typography id="footer-message" variant="subtitle2">
                    <div className="footer-copyright">
                        <Link href={dashboard}>
                            <img
                                style={{
                                    width: '126px',
                                    height: '25px',
                                    marginRight: '30px',
                                }}
                                id="footer-logo"
                                alt="API Catalog"
                                src={productImage}
                            />
                        </Link>
                        <div>
                            <div>
                                Copyright © 2005-2024 Broadcom. All Rights Reserved. The term “Broadcom” refers to
                                Broadcom Inc. and/or its subsidiaries.
                            </div>
                            <div>
                                <Link
                                    rel="noopener noreferrer"
                                    target="_blank"
                                    href="https://www.zowe.org/"
                                    className="footerZoweLinks"
                                >
                                    <u>Zowe</u>
                                </Link>
                                <sup>&reg;</sup>, the Zowe logo and the&nbsp;
                                <Link
                                    rel="noopener noreferrer"
                                    target="_blank"
                                    href="https://openmainframeproject.org/ "
                                    className="footerZoweLinks"
                                >
                                    <u>Open Mainframe Project</u>
                                </Link>
                                &nbsp;are trademarks of&nbsp;
                                <Link
                                    rel="noopener noreferrer"
                                    target="_blank"
                                    href="https://www.linuxfoundation.org/ "
                                    className="footerZoweLinks"
                                >
                                    <u>The Linux Foundation</u>
                                </Link>
                            </div>
                        </div>
                    </div>
                    <div className="footer-policy">
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/legal/privacy-policy"
                        >
                            {' '}
                            Privacy Policy |
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/legal/privacy/cookie-policy"
                        >
                            {' '}
                            Cookies Policy |
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/legal/privacy/data-transfers"
                        >
                            {' '}
                            Data Processing and Data Transfers |
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/citizenship/supplier-responsibility#supply"
                        >
                            {' '}
                            Supplier Responsibility |
                        </Link>
                        <Link
                            rel="noopener noreferrer"
                            target="_blank"
                            href="https://www.broadcom.com/company/legal/terms-of-use/"
                        >
                            {' '}
                            Terms of Use
                        </Link>
                    </div>
                </Typography>
            </footer>
        );
    }
}
