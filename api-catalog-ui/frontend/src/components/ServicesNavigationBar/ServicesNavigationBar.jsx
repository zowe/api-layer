/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { Component } from 'react';
import { Typography } from '@material-ui/core';
import SidebarLink from './NavigationBarLink';
import Shield from '../ErrorBoundary/Shield/Shield';
import SearchCriteria from '../Search/SearchCriteria';

export default class ServicesNavigationBar extends Component {
    render() {
        const { allContainers } = this.props;
        // eslint-disable-next-line no-console
        console.log(allContainers);
        return (
            <div className="sidebar">
                <Shield title="Search Bar is broken !">
                    <SearchCriteria id="search-input" placeholder="Search for APIs..." doSearch={this.handleSearch} />
                </Shield>
                <Typography id="serviceIdTabs" variant="h5">
                    Product APIs
                </Typography>
                {allContainers.map((itemType) => (
                    <SidebarLink text={itemType} />
                ))}
            </div>
        );
    }
}
