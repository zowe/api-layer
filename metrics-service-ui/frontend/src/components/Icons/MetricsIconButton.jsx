import { IconButton } from '@material-ui/core';
import { withStyles } from '@material-ui/core/styles';

import MetricsLogo from '../../assets/images/login_background.jpg'; // TODO real logo

const CustomIconButton = withStyles(() => ({
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

const dashboard = '/metrics-service/ui/v1/#/dashboard';

const MetricsServiceIconButton = (props) => (
    <CustomIconButton href={dashboard} {...props}>
        <img src={MetricsLogo} alt="Metrics Service icon" />
    </CustomIconButton>
);

export default MetricsServiceIconButton;
