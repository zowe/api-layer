import React, { Component } from 'react';
import { Button, Link, Text, Tooltip } from 'mineral-ui';
import { IconPowerSettingsNew } from 'mineral-ui-icons';
import productImage from '../../assets/images/api-catalog-logo.png';
import './Header.css';

export default class Header extends Component {
    handleLogout = () => {
        const { logout } = this.props;
        logout();
    };

    render() {
        const iconLogout = <IconPowerSettingsNew />;
        const dashboard = '/#/dashboard';
        return (
            <div className="header">
                <div className="product-name">
                    <div className="app-icon-container">
                        <img id="logo" alt="API Catalog Product Name" src={productImage} />
                    </div>
                    <Tooltip content="Go to API Catalog dashboard">
                        <Link href={dashboard}>
                            <Text element="h3" color="#ffffff">
                                API Catalog
                            </Text>
                        </Link>
                    </Tooltip>
                </div>
                <div className="right-icons">
                    <div className="logout-container">
                        <Tooltip content="Logout">
                            <Button
                                iconStart={iconLogout}
                                data-testid="logout"
                                minimal
                                circular
                                onClick={this.handleLogout}
                            />
                        </Tooltip>
                    </div>
                </div>
            </div>
        );
    }
}
