import React, { Component } from 'react';
import { Link } from 'mineral-ui';

import logo from '../../assets/images/ca-broadcom-logo.svg';
import './footer.css';

export default class Footer extends Component {
    render() {
        if (process.env.REACT_APP_CA_ENV === 'false') {
            return null;
        }
        return (
            <footer>
                <div className="left">
                    <img src={logo} alt="CA technologies, a Broadcom company" id="footerLogo" />
                    <p>
                        Copyright &copy; 2019 Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc.
                        and/or its subsidiaries.
                    </p>
                </div>
                <div className="right">
                    <Link href="https://support.ca.com/us.html">CA Support</Link>
                </div>
            </footer>
        );
    }
}
