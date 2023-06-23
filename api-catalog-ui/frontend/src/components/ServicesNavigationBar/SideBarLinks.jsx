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
import { Link as RouterLink } from 'react-router-dom';

function SideBarLinks({ storeCurrentTileId, originalTiles, text, match, services }) {
    const handleTabClick = (value) => {
        const correctTile = originalTiles.find((tile) => tile.services.some((service) => service.serviceId === value));
        if (correctTile) {
            storeCurrentTileId(correctTile.id);
        }
    };
    return (
        <Tabs
            TabIndicatorProps={{
                style: { background: 'transparent' },
            }}
            value={text}
            variant="scrollable"
            scrollButtons="auto"
            visibleScrollbar="true"
        >
            <Tab
                onClick={() => handleTabClick(services)}
                value={text}
                className="tabs"
                component={RouterLink}
                to={`${match.url}/${services}`}
                label={text}
                wrapped="false"
            />
        </Tabs>
    );
}
export default SideBarLinks;
