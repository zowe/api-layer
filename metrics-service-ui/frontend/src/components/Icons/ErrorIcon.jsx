import ErrorIcon from '@material-ui/icons/Error';
import { withStyles } from '@material-ui/core/styles';

const CustomErrorIcon = withStyles((theme) => ({
    root: {
        size: '2rem',
        color: theme.palette.error.main,
    },
}))(ErrorIcon);

export default CustomErrorIcon;
