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
    const [currentStream, setCurrentStream] = useState('DISCOVERABLECLIENT');

    useEffect(() => {
        setTimeout(() => {
            window.addStreams(`https://localhost:10010/metrics-service/sse/v1/turbine.stream?cluster=${currentStream}`);
        }, 0);
    }, [currentStream]);

    const handleChange = (event) => {
        setCurrentStream(event.target.value);
    };

    return (
        <React.Fragment>
            <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
                Metrics Service
            </Typography>
            <FormControl className={classes.formControl}>
                <InputLabel id="demo-simple-select-label">Stream</InputLabel>
                <Select
                    labelId="demo-simple-select-label"
                    id="demo-simple-select"
                    value={currentStream}
                    onChange={handleChange}
                >
                    <MenuItem value="DISCOVERABLECLIENT">DISCOVERABLECLIENT</MenuItem>
                    <MenuItem value="APICATALOG">APICATALOG</MenuItem>
                </Select>
            </FormControl>
            <Container maxWidth="lg" id="content" className="dependencies" />
        </React.Fragment>
    );
}
