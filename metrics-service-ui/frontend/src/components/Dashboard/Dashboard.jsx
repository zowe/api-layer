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
import React, { useEffect, useState } from 'react';
import Button from '@material-ui/core/Button';
import Grid from "@material-ui/core/Grid";
import IFRAME  from '../Iframe/IFRAME';
import {Default_Dashboard} from "../Default_Dashboard/Default_dashboard";

export default function Dashboard() {
    const [panelShow, setPanelShow] = useState(false);
    const [defaultPanelShow, setDefaultPanelShow] = useState(true);

    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            <Grid container justify="center">
                <Button variant="outlined" size="large" onClick={() => setPanelShow(!panelShow)}>Grafana Panel</Button>
                <Button variant="outlined" size="large" onClick={() => setDefaultPanelShow(!defaultPanelShow)}>Default Panel</Button>
            </Grid>
            {panelShow ? <IFRAME /> : null}
            {defaultPanelShow ? <Default_Dashboard /> : null}
        </React.Fragment>
    );
}
