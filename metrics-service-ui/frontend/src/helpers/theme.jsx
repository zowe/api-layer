/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

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
    icon: {
        size: 48,
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
