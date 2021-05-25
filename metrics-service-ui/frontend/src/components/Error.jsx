import { Typography } from '@material-ui/core';
import { withStyles } from '@material-ui/core/styles';
import ErrorIcon from '@material-ui/icons/Error';

const CustomErrorIcon = withStyles((theme) => ({
    root: {
        size: '2rem',
        color: theme.palette.error.main,
    },
}))(ErrorIcon);

const ErrorTypography = withStyles(() => ({
    root: {
        fontSize: 20,
        fontWeight: 500,
    },
}))(Typography);

const Error = (props) => (
    <ErrorTypography {...props}>
        <CustomErrorIcon {...props} /> {props.text}
    </ErrorTypography>
);

export default Error;
