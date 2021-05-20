import React, { Component } from 'react';
import { Button, IconButton, Link, Tooltip } from '@material-ui/core';
import PowerSettingsNewIcon from '@material-ui/icons/PowerSettingsNew';

import './Header.css';

export default class Header extends Component {
    handleLogout = () => {
        const { logout } = this.props;
        logout();
    };

    render() {
        const dashboard = '/metrics-service/ui/v1/#/dashboard';
        return (
            <div className="header">
                <div className="product-name">
                    <Link href={dashboard}>
                        <div className="app-icon-container">
                            <img id="logo" alt="Metrics Service Name" />
                        </div>
                    </Link>
                    <Button element="h3" color="#ffffff" href={dashboard}>
                        Metrics Service
                    </Button>
                </div>
                <div className="right-icons">
                    <div className="logout-container">
                        <IconButton>
                            <PowerSettingsNewIcon data-testid="logout" primary circular onClick={this.handleLogout} />
                        </IconButton>
                    </div>
                </div>
            </div>
        );
    }
}
