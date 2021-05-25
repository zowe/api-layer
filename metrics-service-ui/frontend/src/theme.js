import { createMuiTheme } from '@material-ui/core/styles';
import blue from '@material-ui/core/colors/blue';
import { red } from '@material-ui/core/colors';

const theme = createMuiTheme({
    palette: {
        primary: {
            main: blue[700],
        },
        header: {
            main: '#FFFFFF',
        },
        background: {
            main: '#FFFFFF',
        },
        error: {
            main: red[500],
        },
    },
    props: {
        MuiTooltip: {
            enterDelay: 300,
            enterNextDelay: 300,
            enterTouchDelay: 300,
        },
    },
    overrides: {
        MuiTooltip: {
            tooltip: {
                fontSize: '1em',
            },
        },
    },
});

export default theme;
