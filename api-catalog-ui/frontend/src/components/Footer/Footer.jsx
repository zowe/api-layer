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

import logo from '../../assets/images/ca-broadcom-logo.svg';
import './footer.css';

export default class Footer extends Component {
    render() {
        if (process.env.REACT_APP_CA_ENV === 'false') {
            return null;
        }
        return (
            <footer id="pageFooter">
                <div className="left">
                    <img src={logo} alt="CA technologies, a Broadcom company" id="footerLogo" />
                    <p>
                        Copyright &copy; 2019 Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc.
                        and/or its subsidiaries.
                    </p>
                </div>
                <div className="right">
                    <Link data-testid="link" href="https://support.broadcom.com">
                        Broadcom Support
                    </Link>
                </div>
            </footer>
        );
    }
}
