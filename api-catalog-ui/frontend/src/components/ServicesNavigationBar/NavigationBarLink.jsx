/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Box, Tab, Tabs } from '@material-ui/core';

function SidebarLink({ text }) {
    const handleChange = (_event, value) => {};
    return (
        <div className="link">
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs variant="scrollable" scrollButtons="auto" value={text} onChange={handleChange}>
                    <Tab label={text} />
                </Tabs>
            </Box>
        </div>
    );
}
export default SidebarLink;
