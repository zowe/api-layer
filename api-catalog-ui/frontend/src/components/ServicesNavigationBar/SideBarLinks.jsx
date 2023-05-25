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
import './ServicesNavigationBar.css';

function SideBarLinks({ text, match, services }) {
    return (
        <Tabs
            TabIndicatorProps={{
                style: { background: 'transparent' },
            }}
            value={text}
            allowScrollButtonsMobile
            variant="scrollable"
            scrollButtons="auto"
        >
            <Tab value={text} className="tabs" component={RouterLink} to={`${match.url}/${services}`} label={text} />
        </Tabs>
    );
}
export default SideBarLinks;
