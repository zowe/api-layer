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
import { makeStyles } from '@material-ui/core/styles';
import FormControl from '@material-ui/core/FormControl';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import InputLabel from '@material-ui/core/InputLabel';
import axios from 'axios';

const useStyles = makeStyles((theme) => ({
    formControl: {
        margin: theme.spacing(1),
        minWidth: 120,
    },
    selectEmpty: {
        marginTop: theme.spacing(2),
    },
}));

export default function Dashboard() {
    const classes = useStyles();

    const [availableClusters, setAvailableClusters] = useState([]);
    const [currentCluster, setCurrentCluster] = useState(null);
    const [haveGottenClusters, setHaveGottenClusters] = useState(false);

    function setMetricsDisplay(cluster) {
        setCurrentCluster(cluster);
        window.addStreams(`${window.location.origin}/metrics-service/sse/v1/turbine.stream?cluster=${cluster}`);
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

    const handleChange = (event) => {
        setMetricsDisplay(event.target.value);
    };

    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            {availableClusters.length > 0 && (
                <FormControl className={classes.formControl}>
                    <InputLabel id="demo-simple-select-label">Stream</InputLabel>
                    <Select
                        labelId="demo-simple-select-label"
                        id="demo-simple-select"
                        value={currentCluster}
                        onChange={handleChange}
                    >
                        {availableClusters.map((stream) => (
                            <MenuItem value={stream}>{stream}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
            )}
            <Container maxWidth="lg" id="content" className="dependencies" />
        </React.Fragment>
    );
}
