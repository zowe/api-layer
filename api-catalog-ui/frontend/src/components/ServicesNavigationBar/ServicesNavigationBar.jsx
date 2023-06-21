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
import SideBarLinks from './SideBarLinks';
import Shield from '../ErrorBoundary/Shield/Shield';
import SearchCriteria from '../Search/SearchCriteria';
import './ServicesNavigationBar.css';

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
        const { match, services, searchCriteria, storeCurrentTileId } = this.props;
        const hasTiles = services && services.length > 0;
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        return (
            <div>
                <div id="search2">
                    <Shield title="Search Bar is broken !">
                        <SearchCriteria
                            data-testid="search-bar"
                            placeholder="Search for APIs..."
                            doSearch={this.handleSearch}
                        />
                    </Shield>
                </div>
                <Typography id="serviceIdTabs" variant="h5">
                    Product APIs
                </Typography>
                {services.map((tile) =>
                    tile.services.map((service) => (
                        <div key={service.serviceId} className="nav-tabs">
                            <SideBarLinks
                                storeCurrentTileId={storeCurrentTileId}
                                originalTiles={services}
                                key={service.serviceId}
                                text={service.title}
                                match={match}
                                services={service.serviceId}
                            />
                        </div>
                    ))
                )}
                {!hasTiles && hasSearchCriteria && (
                    <Typography id="search_no_results" variant="subtitle2" style={{ color: '#1d5bbf' }}>
                        No services found matching search criteria
                    </Typography>
                )}
            </div>
        );
    }
}