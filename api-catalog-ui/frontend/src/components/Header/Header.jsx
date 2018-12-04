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
        const iconLogout = <IconPowerSettingsNew color="white" />;
        const dashboard = '/ui/v1/apicatalog/#/dashboard';
        return (
            <div className="header">
                <div className="product-name">
                    <Link href={dashboard}>
                        <div className="app-icon-container">
                            <img id="logo" alt="API Catalog Product Name" src={productImage} />
                        </div>
                    </Link>
                    <Link href={dashboard}>
                        <Text element="h3" color="#ffffff">
                            API Catalog
                        </Text>
                    </Link>
                </div>
                <div className="right-icons">
                    <div className="logout-container">
                        <Tooltip content="Logout">
                            <Button
                                iconStart={iconLogout}
                                data-testid="logout"
                                primary
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
