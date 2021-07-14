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
import Container from '@material-ui/core/Container';
import React, { useEffect } from 'react';
// import HystrixCommandMonitor from './HystrixCommandMonitor';
// import addStreams from './monitor';
import axios from 'axios';
import './hystrixCommand.css';

export default function Dashboard() {
    useEffect(() => {
        // eslint-disable-next-line no-console
        console.log(`${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_METRICS_HOME}`);
        axios.get('https://localhost:10010/api/v1/metrics-service/clusters', { withCredentials: true }).then((res) => {
            // eslint-disable-next-line no-console
            console.log(res.data);
        });
        setTimeout(() => {
            window.addStreams('https://localhost:10019/metrics-service/turbine.stream?cluster=DISCOVERABLECLIENT');
        }, 0);
    }, []);
    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            <Container maxWidth="lg" id="content" className="dependencies" />
        </React.Fragment>
    );
}
