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
    getServices() {
        const { originalTiles } = this.props;
        const services = [];
        originalTiles.forEach((tile) => {
            tile.services.forEach((service) => {
                services.push(service.title);
            });
        });
        return services;
    }

    handleSearch = (value) => {
        // eslint-disable-next-line no-console
        console.log(value);
        const { filterText } = this.props;
        filterText(value);
    };

    render() {
        const { match } = this.props;

        const { originalTiles } = this.props;
        const services2 = [];
        originalTiles.forEach((tile) => {
            tile.services.forEach((service) => {
                services2.push(service.serviceId);
            });
        });
        const services = this.getServices();
        return (
            <div className="sidebar">
                <Shield title="Search Bar is broken !">
                    <SearchCriteria id="search-input" placeholder="Search for APIs..." doSearch={this.handleSearch} />
                </Shield>
                <Typography id="serviceIdTabs" variant="h5">
                    Product APIs
                </Typography>
                {services.map((itemType) => (
                    <SidebarLink text={itemType} match={match} services={services2} />
                ))}
            </div>
        );
    }
}
