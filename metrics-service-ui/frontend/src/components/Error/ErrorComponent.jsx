/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { Typography } from '@material-ui/core';
import ErrorIcon from '@material-ui/icons/Error';
import { withStyles } from '@material-ui/core/styles';

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

function ErrorComponent(props) {
    return (
        <ErrorTypography {...props}>
            <CustomErrorIcon id="erroricon" {...props} /> {props.text}
        </ErrorTypography>
    );
}

export default ErrorComponent;
