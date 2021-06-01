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
import { withStyles } from '@material-ui/core/styles';

import MetricsLogo from '../../assets/images/login_background.jpg';

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

const MetricsIconButton = (props) => (
    <CustomIconButton href={dashboard} {...props}>
        <img src={MetricsLogo} alt="Metrics Service icon" />
    </CustomIconButton>
);

export default MetricsIconButton;
