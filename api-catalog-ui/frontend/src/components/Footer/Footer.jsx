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
import { Link } from '@material-ui/core';

export default class Footer extends Component {
    render() {
        if (process.env.REACT_APP_CA_ENV === 'false') {
            return null;
        }
        return (
            <footer id="pageFooter">
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
            </footer>
        );
    }
}
