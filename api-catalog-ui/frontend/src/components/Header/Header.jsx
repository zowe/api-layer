import { Component } from 'react';
import { Button, Link, Typography, Tooltip } from '@material-ui/core';
import PowerSettingsNewIcon from '@material-ui/icons/PowerSettingsNew';
import productImage from '../../assets/images/api-catalog-logo.png';
import './Header.css';

export default class Header extends Component {
    handleLogout = () => {
        const { logout } = this.props;
        logout();
    };

    render() {
        const iconLogout = <PowerSettingsNewIcon style={{ color: 'white' }} />;
        const dashboard = 'ui/v1/apicatalog/#/dashboard';
        return (
            <div className="header">
                <div className="product-name">
                    <Link href={dashboard}>
                        <div className="app-icon-container">
                            <img id="logo" alt="API Catalog Product Name" src={productImage} />
                        </div>
                    </Link>
                    <Link href={dashboard}>
                        <Typography variant="h7">API Catalog</Typography>
                    </Link>
                </div>
                <div className="right-icons">
                    <div className="logout-container">
                        <Button data-testid="logout" primary circular onClick={this.handleLogout}>
                            <Tooltip circular title="Logout">
                                {iconLogout}
                            </Tooltip>
                        </Button>
                    </div>
                </div>
            </div>
        );
    }
}
