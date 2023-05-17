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

function getServices(originalTiles, tiles, services) {
    originalTiles.forEach((tile) => {
        tile.services.forEach((service) => {
            tiles.push(service.title);
        });
    });
    originalTiles.forEach((tile) => {
        tile.services.forEach((service) => {
            services.push(service.serviceId);
        });
    });
}

export default class ServicesNavigationBar extends Component {
    componentWillUnmount() {
        const { clear } = this.props;
        clear();
    }

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    render() {
        const { match } = this.props;
        const { originalTiles, searchCriteria } = this.props;
        const tiles = [];
        const services = [];
        getServices(originalTiles, tiles, services);
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        const hasServices = services && services.length > 0;
        return (
            <div className="sidebar">
                <Shield title="Search Bar is broken !">
                    <SearchCriteria id="search-input" placeholder="Search for APIs..." doSearch={this.handleSearch} />
                </Shield>
                <Typography id="serviceIdTabs" variant="h5">
                    Product APIs
                </Typography>
                {tiles.map((itemType) => (
                    <SidebarLink text={itemType} match={match} services={services} />
                ))}
                {!hasServices && hasSearchCriteria && (
                    <Typography id="search_no_results" variant="subtitle2" style={{ color: '#1d5bbf' }}>
                        No services found matching search criteria
                    </Typography>
                )}
            </div>
        );
    }
}
