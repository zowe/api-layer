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
import React, { useEffect, useState } from 'react';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Box from '@material-ui/core/Box';
import axios from 'axios';

export default function Dashboard() {
    const [availableClusters, setAvailableClusters] = useState([]);
    const [currentCluster, setCurrentCluster] = useState(null);
    const [haveGottenClusters, setHaveGottenClusters] = useState(false);

    function setMetricsDisplay(cluster) {
        setCurrentCluster(cluster);
        window.addStreams(
            `${window.location.origin}/metrics-service/sse/v1/turbine.stream?cluster=${cluster}`,
            cluster
        );
    }

    function retrieveAvailableClusters(callback) {
        axios.get(`${window.location.origin}/metrics-service/api/v1/clusters`).then((res) => {
            callback(res);
        });
    }

    useEffect(() => {
        if (!haveGottenClusters && availableClusters.length === 0) {
            retrieveAvailableClusters((res) => {
                setHaveGottenClusters(true);

                const clusters = res.data.map((d) => d.name);
                setAvailableClusters(clusters);

                if (clusters.length > 0) {
                    setMetricsDisplay(clusters[0]);
                }
            });
        }

        setTimeout(() => {
            retrieveAvailableClusters((res) => {
                const clusters = res.data.map((d) => d.name);
                setAvailableClusters(clusters);
            });
        }, 30000);
    });

    const handleChange = (event, newValue) => {
        setMetricsDisplay(newValue);
    };

    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            {availableClusters.length > 0 && (
                <Box sx={{ width: '100%' }}>
                    <Box sx={{ borderBottom: 1 }}>
                        <Tabs value={currentCluster} onChange={handleChange} aria-label="service tabs" centered>
                            {availableClusters.map((stream) => (
                                <Tab label={stream} value={stream} />
                            ))}
                        </Tabs>
                    </Box>
                </Box>
            )}
            <Container maxWidth="lg" id="content" className="dependencies" />
        </React.Fragment>
    );
}
