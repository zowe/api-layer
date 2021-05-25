import React from 'react';
import { IconButton, Tooltip, Link } from '@material-ui/core';
import { withStyles } from '@material-ui/core/styles';
import PowerSettingsNewIcon from '@material-ui/icons/PowerSettingsNew';

import MetricsLogo from '../../assets/images/login_background.jpg'; // TODO real logo

const LogoutIconButton = withStyles((theme) => ({
    root: {
        color: theme.palette.header.main,
        padding: 10,
        margin: 10,
        marginRight: 40,
        backgroundColor: theme.palette.primary.light,
        '&:hover': {
            backgroundColor: theme.palette.primary.dark,
        },
    },
}))(IconButton);

const LogoutIcon = withStyles(() => ({
    root: {
        fontSize: 25,
    },
}))(PowerSettingsNewIcon);

const MetricsIconButton = withStyles(() => ({
    root: {
        height: 48,
        width: 48,
        margin: 10,
        marginLeft: 20,
        padding: 0,
        '&:hover': {
            backgroundColor: 'transparent',
        },
    },
}))(IconButton);

const ServiceNameHeader = withStyles((theme) => ({
    root: {
        color: theme.palette.header.main,
        'align-self': 'center',
        width: '100%',
        margin: 20,
    },
}))(Link);

const Header = (props) => {
    const handleLogout = () => {
        const { logout } = props;
        logout();
    };

    const dashboard = '/metrics-service/ui/v1/#/dashboard';

    return (
        <div className="header">
            <MetricsIconButton href={dashboard}>
                <img src={MetricsLogo} alt="Metrics Service icon" />
            </MetricsIconButton>
            <ServiceNameHeader variant="h6" align="left" underline="none" href={dashboard}>
                Metrics Service
            </ServiceNameHeader>
            <Tooltip title="Logout">
                <LogoutIconButton onClick={handleLogout}>
                    <LogoutIcon />
                </LogoutIconButton>
            </Tooltip>
        </div>
    );
};

export default Header;
