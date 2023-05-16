/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Box, Tab, Tabs, Typography } from '@material-ui/core';
import { NavTab } from 'react-router-tabs';

function SidebarLink({ text, match, services, servicesTitle }) {
    const handleChange = (_event, value) => {};
    return (
        <div className="link">
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs variant="scrollable" scrollButtons="auto" value={text} onChange={handleChange}>
                    <Tab label={text} />
                </Tabs>
            </Box>
            {/* TODO use navbar rather */}
            {/* <NavTab sx={{ borderBottom: 1, borderColor: 'divider' }}> */}
            {/*    <Tabs variant="scrollable" scrollButtons="auto" value={servicesTitle} onChange={handleChange}> */}
            {/*        <Tab label={servicesTitle} /> */}
            {/*    </Tabs> */}
            {/* </NavTab> */}
            {/* {services.map((serviceId) => ( */}
            {/*    <NavTab sx={{ borderBottom: 1, borderColor: 'divider' }} to={`${match.url}/${serviceId}`}> */}
            {/*        <Tabs variant="scrollable" scrollButtons="auto" value={serviceId} onChange={handleChange}> */}
            {/*            <Tab label={servicesTitle} /> */}
            {/*        </Tabs> */}
            {/*    </NavTab> */}
            {/* ))} */}
        </div>
    );
}
export default SidebarLink;
