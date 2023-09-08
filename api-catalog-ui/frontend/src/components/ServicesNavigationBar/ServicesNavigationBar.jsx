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
import { Tab, Tabs, Tooltip, Typography, withStyles, Button } from '@material-ui/core';
import { Link as RouterLink } from 'react-router-dom';
import Shield from '../ErrorBoundary/Shield/Shield';
import SearchCriteria from '../Search/SearchCriteria';
import { closeMobileMenu } from '../../utils/utilFunctions';
import MenuCloseImage from '../../assets/images/xmark.svg';

export default class ServicesNavigationBar extends Component {
    componentWillUnmount() {
        const { clear } = this.props;
        clear();
    }

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    handleTabChange = (event, selectedTab) => {
        localStorage.removeItem('serviceId');
        localStorage.setItem('selectedTab', selectedTab);
    };

    handleTabClick = (id) => {
        const { storeCurrentTileId, services } = this.props;
        const correctTile = services.find((tile) => tile.services.some((service) => service.serviceId === id));
        if (correctTile) {
            storeCurrentTileId(correctTile.id);
            closeMobileMenu();
        }
    };

    styles = () => ({
        truncatedTabLabel: {
            maxWidth: '100%',
            width: 'max-content',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
        },
    });

    render() {
        const { match, services, searchCriteria } = this.props;
        const hasTiles = services && services.length > 0;
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        let selectedTab = Number(localStorage.getItem('selectedTab'));
        let allServices;
        let allServiceIds;
        if (hasTiles) {
            allServices = services.flatMap((tile) => tile.services);
            allServiceIds = allServices.map((service) => service.serviceId);
            if (localStorage.getItem('serviceId')) {
                const id = localStorage.getItem('serviceId');
                if (allServiceIds.includes(id)) {
                    selectedTab = allServiceIds.indexOf(id);
                }
            }
        }
        const TruncatedTabLabel = withStyles(this.styles)(({ classes, label }) => (
            <Tooltip title={label} placement="bottom">
                <div className={classes.truncatedTabLabel}>{label}</div>
            </Tooltip>
        ));
        return (
            <div>
                <div className="mobile-view mobile-menu-close-ctn">
                    <h2 className="title1">API Catalog</h2>
                    <Button
                        className="mobile-menu-close-btn icon-btn"
                        aria-label="close-menu"
                        onClick={closeMobileMenu}
                    >
                        <img alt="Menu" src={MenuCloseImage} className="mobile-menu-close" />
                    </Button>
                </div>
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
                {!hasTiles && hasSearchCriteria && (
                    <Typography id="search_no_results" variant="subtitle2">
                        No services found matching search criteria
                    </Typography>
                )}
                {hasTiles && (
                    <Tabs
                        value={selectedTab || 0}
                        onChange={this.handleTabChange}
                        variant="scrollable"
                        orientation="vertical"
                        scrollButtons="auto"
                        className="custom-tabs"
                    >
                        {allServices.map((service, serviceIndex) => (
                            <Tab
                                onClick={() => this.handleTabClick(service.serviceId)}
                                key={service.serviceId}
                                className="tabs"
                                component={RouterLink}
                                to={`${match.url}/${service.serviceId}`}
                                value={serviceIndex}
                                label={<TruncatedTabLabel label={service.title} />}
                                wrapped
                            />
                        ))}
                    </Tabs>
                )}
            </div>
        );
    }
}
