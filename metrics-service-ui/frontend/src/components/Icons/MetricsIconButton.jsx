/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { IconButton } from '@material-ui/core';
import { makeStyles, withStyles } from '@material-ui/core/styles';

import MetricsLogo from '../../assets/images/metrics_icon_white.svg';

const useStyles = makeStyles((theme) => ({
    svgPath: {
        fill: theme.palette.background.main,
    },
}));

const CustomIconButton = withStyles((theme) => ({
    root: {
        height: theme.icon.size,
        width: theme.icon.size,
        margin: 10,
        marginLeft: 20,
        padding: 0,
        backgroundColor: 'transparent',
    },
}))(IconButton);

const dashboard = '/metrics-service/ui/v1/#/dashboard';

const MetricsIconButton = (props) => {
    const classes = useStyles();

    return (
        <CustomIconButton href={dashboard} {...props}>
            <svg viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M12.6667 2H3.33333C2.6 2 2 2.6 2 3.33333V12.6667C2 13.4 2.6 14 3.33333 14H12.6667C13.4 14 14 13.4 14 12.6667V3.33333C14 2.6 13.4 2 12.6667 2ZM12.6667 12.6667H3.33333V3.33333H12.6667V12.6667Z"
                    className={classes.svgPath}
                />
                <path d="M6 8H4.66667V11.3333H6V8Z" className={classes.svgPath} />
                <path d="M11.3333 4.66667H10V11.3333H11.3333V4.66667Z" className={classes.svgPath} />
                <path d="M8.66667 9.33333H7.33333V11.3333H8.66667V9.33333Z" className={classes.svgPath} />
                <path d="M8.66667 6.66667H7.33333V8H8.66667V6.66667Z" className={classes.svgPath} />
            </svg>
        </CustomIconButton>
    );
};

export default MetricsIconButton;
