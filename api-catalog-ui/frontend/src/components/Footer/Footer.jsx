import React, { Component } from 'react';
import { Link } from 'mineral-ui';

import caLogo from '../../assets/images/CA-Logo.svg';
import './footer.css';

export default class Footer extends Component {
    render() {
        return (
            <footer>
                <div className="left">
                    <img src={caLogo} alt="" />
                    CA API Catalog
                </div>
                <div className="right">
                    <p>&copy; 2018 CA Technologies. All Rights Reserved.</p>
                    <Link href="https://support.ca.com/us.html">CA Support</Link>
                </div>
            </footer>
        );
    }
}
