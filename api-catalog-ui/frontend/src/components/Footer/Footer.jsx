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

export default class Footer extends Component {
    render() {
        if (isAPIPortal() || process.env.REACT_APP_CA_ENV === 'true') {
            return (
                <footer id="pageFooter">
                    <div id="bottom-info-div">
                        <Container>
                            <strong>Capabilities</strong>
                            <Link className="links" />
                        </Container>
                        <Container>
                            <strong>News & Information</strong>
                            <Link className="links">Support Portal</Link>
                        </Container>
                        <Container>
                            <strong>Contact Us</strong>
                            <Link className="links" />
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
