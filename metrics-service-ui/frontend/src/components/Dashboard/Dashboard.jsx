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
import './hystrixCommand.css';

export default function Dashboard() {
    useEffect(() => {
        // const hystrixMonitor = new HystrixCommandMonitor(0, 'content', { includeDetailIcon: false });
        // // start the EventSource which will open a streaming connection to the server
        // const source = new EventSource('https://localhost:10012/discoverableclient/application/hystrix.stream');
        // // add the listener that will process incoming events
        // source.addEventListener('message', hystrixMonitor.eventSourceMessageListener, false);
        setTimeout(() => {
            window.addStreams();
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
