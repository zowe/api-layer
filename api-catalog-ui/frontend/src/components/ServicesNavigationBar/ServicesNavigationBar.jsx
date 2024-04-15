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
import { Tab, Tabs, Tooltip, Typography, withStyles, Button, IconButton } from '@material-ui/core';
import { Link as RouterLink } from 'react-router-dom';
import PropTypes from 'prop-types';
import Shield from '../ErrorBoundary/Shield/Shield';
import SearchCriteria from '../Search/SearchCriteria';
import { closeMobileMenu, isAPIPortal, findAndFormatZowe } from '../../utils/utilFunctions';
import MenuCloseImage from '../../assets/images/xmark.svg';
import { ReactComponent as BackArrowImage } from '../../assets/images/angles-left.svg';
import { sortServices } from '../../selectors/selectors';
import HeaderDetail from '../Header/HeaderDetail';

export default class ServicesNavigationBar extends Component {
    componentDidMount() {
        window.addEventListener('popstate', this.handlePopstate);
    }

    componentWillUnmount() {
        window.removeEventListener('popstate', this.handlePopstate);
        const { clear } = this.props;
        clear();
    }

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    handleTabClick = (id) => {
        const { storeCurrentTileId, services } = this.props;
        const correctTile = services.find((tile) => tile.services.some((service) => service.serviceId === id));
        if (correctTile) {
            storeCurrentTileId(correctTile.id);
            closeMobileMenu();
        }
    };

    handlePopstate = () => {
        const { services, storeCurrentTileId } = this.props;
        const url = window.location.href;
        if (url?.includes('/service')) {
            const parts = url.split('/');
            const serviceId = parts[parts.length - 1];
            const correctTile = services.find((tile) =>
                tile.services.some((service) => service.serviceId === serviceId)
            );
            if (correctTile) {
                storeCurrentTileId(correctTile.id);
            }
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
        const url = window.location.href;
        const parts = url.split('/');
        const serviceId = parts[parts.length - 1];
        const dashboard = '#/dashboard';
        const backButtonText = 'Back';
        let selectedTab = Number(0);
        let allServices;
        if (hasTiles) {
            allServices = sortServices(services);
            const index = allServices.findIndex((item) => item.serviceId === serviceId);
            selectedTab = Number(index);
        }
        const TruncatedTabLabel = withStyles(this.styles)(({ classes, label }) => (
            <Tooltip title={label} placement="bottom">
                <div className={classes.truncatedTabLabel}>{findAndFormatZowe(label)}</div>
            </Tooltip>
        ));
        return (
            <div>
                {isAPIPortal() && (
                    <IconButton
                        id="go-back-button-portal"
                        className="button-link"
                        onClick={closeMobileMenu}
                        size="medium"
                        href={dashboard}
                    >
                        <BackArrowImage className="icon-img" alt="" />
                        {backButtonText}
                    </IconButton>
                )}
                {isAPIPortal() && (
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
                )}
                <div id="search2">
                    <Shield title="Search Bar is broken !">
                        <SearchCriteria data-testid="search-bar" placeholder="Search..." doSearch={this.handleSearch} />
                    </Shield>
                </div>
                <Typography id="serviceIdTabs" variant="h5">
                    Product APIs
                </Typography>
                {!hasTiles && hasSearchCriteria && (
                    <Typography id="search_no_results" variant="subtitle2" className="no-content">
                        No services found matching search criteria
                    </Typography>
                )}
                {hasTiles && (
                    <Tabs
                        value={selectedTab}
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
                {isAPIPortal() && (
                    <div className="mobile-view header-menu">
                        <HeaderDetail />
                    </div>
                )}
            </div>
        );
    }
}

ServicesNavigationBar.propTypes = {
    storeCurrentTileId: PropTypes.func.isRequired,
    services: PropTypes.shape({
        find: PropTypes.func.isRequired,
    }).isRequired,
};
