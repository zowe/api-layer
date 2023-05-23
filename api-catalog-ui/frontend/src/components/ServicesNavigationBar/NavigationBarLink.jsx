/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Tab, Tabs } from '@material-ui/core';
import { NavTab } from 'react-router-tabs';

function SidebarLink({ storeCurrentTileId, fetchTilesStart, tileId, text, match, services }) {
    const handleChange = () => {
        storeCurrentTileId(tileId);
        fetchTilesStart(tileId);
    };
    return (
        <div className="link">
            <NavTab sx={{ borderBottom: 1, borderColor: 'divider' }} to={`${match.url}/${services}`}>
                <Tabs variant="scrollable" to={`${match.url}/${services}`} scrollButtons="auto" onChange={handleChange}>
                    <Tab label={text} />
                </Tabs>
            </NavTab>
        </div>
    );
}
export default SidebarLink;
