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
