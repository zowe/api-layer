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
import { Container, Link, Typography } from '@material-ui/core';
import { isAPIPortal } from '../../utils/utilFunctions';
import linkedInImg from '../../assets/images/linkedin-red.png';
import twitterImg from '../../assets/images/twitter-square.png';
import youtubeImg from '../../assets/images/youtube-square.png';
import mailImg from '../../assets/images/square-envelope.png';

export default class Footer extends Component {
    render() {
        if (isAPIPortal() || process.env.REACT_APP_CA_ENV === 'true') {
            return (
                <footer id="pageFooter">
                    <div id="bottom-info-div">
                        <Container>
                            <strong />
                            <Link className="links" />
                            <Link className="links" />
                        </Container>
                        <Container>
                            <strong>News & Information</strong>
                            <Link className="links" />
                            <Link className="links" />
                            <Link className="links" />
                            <Link className="links" />
                            <Link className="links" />
                            <Link className="links" />
                        </Container>
                        <Container>
                            <strong>Contact Us</strong>
                            <Link className="footer-links" style={{ paddingRight: '5px' }}>
                                <img id="linkedin" alt="linkedin" src={linkedInImg} />
                            </Link>
                            <Link className="footer-links" style={{ paddingRight: '5px' }}>
                                <img id="twitter" alt="twitter" src={twitterImg} />
                            </Link>
                            <Link className="footer-links" style={{ paddingRight: '5px' }}>
                                <img id="youtube" alt="youtube" src={youtubeImg} />
                            </Link>
                            <Link className="footer-links">
                                <img id="email" alt="email" src={mailImg} />
                            </Link>
                        </Container>
                    </div>
                    <div className="left">
                        <img alt="" id="footerLogo" />
                        <Link className="footer-links" />
                        <br />
                        <br />
                        <p />
                    </div>
                    <div className="right">
                        <Link data-testid="link" />
                    </div>
                    <Typography id="footer-message" variant="subtitle2" />
                </footer>
            );
        }
        return null;
    }
}
