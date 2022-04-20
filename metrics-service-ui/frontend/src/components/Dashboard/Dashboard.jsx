/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import Typography from '@material-ui/core/Typography';
import React, { useState } from 'react';
import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import GatewayHttpDashboard from '../grafana-iframes/Gateway-http-dashboard';
import GatewayZebraDashboard from '../grafana-iframes/Gateway-zebra-dashboard';
import { DefaultDashboard } from '../Default_Dashboard/DefaultDashboard';

export default function Dashboard() {
    const [zebraPanelShow, setZebraPanelShow] = useState(false);
    const [httpPanelShow, setHttpPanelShow] = useState(false);
    const [defaultPanelShow, setDefaultPanelShow] = useState(true);

    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            <Grid container justify="center" columnSpacing={2}>
                <Button variant="outlined" size="large" onClick={() => setHttpPanelShow(!httpPanelShow)}>
                    Http Metrics Panel
                </Button>
                <Button variant="outlined" size="large" onClick={() => setZebraPanelShow(!zebraPanelShow)}>
                    Zebra Metrics Panel
                </Button>
                <Button variant="outlined" size="large" onClick={() => setDefaultPanelShow(!defaultPanelShow)}>
                    Default Panel
                </Button>
            </Grid>
            {httpPanelShow ? <GatewayHttpDashboard /> : null}
            {zebraPanelShow ? <GatewayZebraDashboard /> : null}
            {defaultPanelShow ? <DefaultDashboard /> : null}
        </React.Fragment>
    );
}
