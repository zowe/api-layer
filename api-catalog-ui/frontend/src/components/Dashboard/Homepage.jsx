/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Typography, Button } from '@material-ui/core';
import React, { Component } from 'react';
import HomepageHeader from '../../assets/images/hero_image.svg';
import BroadcomLogo from '../../assets/images/broadcom_logo.svg';
import Code from '../../assets/images/code.svg';
import ZoweLogo from '../../assets/images/zowe_logo.svg';
import Footer from '../Footer/Footer';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import HomepageTile from '../Tile/HomepageTile';
import { isAPIPortal, closeMobileMenu, customUIStyle } from '../../utils/utilFunctions';
import MenuCloseImage from '../../assets/images/xmark.svg';
import { sortServices } from '../../selectors/selectors';

const loadFeedbackButton = () => {
    if (isAPIPortal()) {
        return import('../FeedbackButton/FeedbackButton');
    }
    return Promise.resolve(null);
};

const FeedbackButton = React.lazy(loadFeedbackButton);

export default class Homepage extends Component {
    componentDidMount() {
        if (isAPIPortal()) {
            document.title = process.env.REACT_APP_API_PORTAL_HOMEPAGE_TITLE;
            const goBackButton = document.getElementById('go-back-button-portal');
            if (goBackButton) {
                goBackButton.style.display = 'none';
            }
        }
        const { fetchTilesStart, clearService } = this.props;
        clearService();
        fetchTilesStart();
    }

    componentWillUnmount() {
        const { fetchTilesStop, clear } = this.props;
        clear();
        fetchTilesStop();
    }

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    redirectToDetailPage = () => {
        const { history, tiles, fetchTilesError } = this.props;
        if (!fetchTilesError && tiles?.length > 0) {
            const firstTile = tiles[0];
            const hasServices = firstTile?.services?.length > 0 && 'serviceId' in firstTile.services[0];

            if (hasServices) {
                history.push(`/dashboard`);
            }
        }
    };

    render() {
        const { tiles, history, searchCriteria, isLoading, fetchTilesError, storeCurrentTileId } = this.props;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        const hasSearchCriteria =
            typeof searchCriteria !== 'undefined' &&
            searchCriteria !== undefined &&
            searchCriteria !== null &&
            searchCriteria.length > 0;
        if (hasTiles && 'customStyleConfig' in tiles[0] && tiles[0].customStyleConfig) {
            customUIStyle(tiles[0].customStyleConfig);
        }
        let allServices;
        if (hasTiles) {
            allServices = sortServices(tiles);
        }
        return (
            <div className="main-content homepage-content">
                {isAPIPortal() && <FeedbackButton />}
                <div className="homepage-header">
                    <div>
                        <h1>Developer Portal Where mainframe development begins</h1>
                        <h3>
                            Streamline automation and orchestration by effortlessly tapping into mainframe services.
                        </h3>
                        <Button
                            className="button-cta"
                            color="primary"
                            size="medium"
                            onClick={this.redirectToDetailPage}
                        >
                            Design Using APIs
                        </Button>
                    </div>
                    <div className="homepage-header-images">
                        <img alt="Homepage Header" src={HomepageHeader} className="" />
                    </div>
                </div>
                <h2 className="header-primary">
                    Your hub for creating seamless integrations that tap into the full capabilities of mainframe
                    services
                </h2>
                <div className="homepage-cards">
                    <div>
                        <Button onClick={this.redirectToDetailPage}>
                            <img alt="Code" src={Code} />
                            <h5>Develop with APIs</h5>
                            <p>Use Standardized, open APIs to secure access to mainframe data and functionality.</p>
                        </Button>
                        <Button target="_blank" href="https://docs.zowe.org">
                            <img alt="Zowe Logo" src={ZoweLogo} />
                            <h5>Zowe Docs</h5>
                            <p>View the official technical documentation for Open Mainframe Project's Zowe.</p>
                        </Button>
                    </div>
                    <div>
                        <Button
                            target="_blank"
                            href="https://community.broadcom.com/mainframesoftware/communities/communityhomeblogs?CommunityKey=149509e5-5ab7-4ce6-b617-dbd664b18882"
                        >
                            <img alt="Code" src={BroadcomLogo} />
                            <h5>Next Generation Mainframers Community</h5>
                            <p>
                                Join the mainframe community to access educational resources, blogs, webcasts, and more
                            </p>
                        </Button>
                        <Button target="_blank" href="https://techdocs.broadcom.com/us/en/ca-mainframe-software.html">
                            <img alt="Broadcom Logo" src={BroadcomLogo} />
                            <h5>Broadcom TechDocs</h5>
                            <p>
                                View comprehensive, up-to-date technical documentation for Broadcom mainframe products.
                            </p>
                        </Button>
                    </div>
                </div>

                <div>
                    <div className="nav-bar dashboard-mobile-menu homepage-menu">
                        {isLoading && <div className="loadingDiv" />}

                        <div className="mobile-menu-close-ctn">
                            <Button
                                className="mobile-menu-close-btn icon-btn"
                                aria-label="close-menu"
                                onClick={closeMobileMenu}
                            >
                                <img alt="Menu" src={MenuCloseImage} className="mobile-menu-close" />
                            </Button>
                        </div>

                        <div id="search">
                            <Shield title="Search Bar is broken !">
                                <SearchCriteria
                                    id="search-input"
                                    placeholder="Search..."
                                    doSearch={this.handleSearch}
                                />
                            </Shield>
                        </div>

                        <Typography id="serviceIdTabs" variant="h5">
                            Product APIs
                        </Typography>
                        <div className="homepage-menu-content">
                            {hasTiles &&
                                allServices.map((service) =>
                                    tiles
                                        .filter((tile) => tile.services.includes(service))
                                        .map((tile) => (
                                            <HomepageTile
                                                storeCurrentTileId={storeCurrentTileId}
                                                service={service}
                                                key={service}
                                                tile={tile}
                                                history={history}
                                            />
                                        ))
                                )}
                            {!hasTiles && hasSearchCriteria && (
                                <Typography id="search_no_results" variant="subtitle2" className="no-content">
                                    No services found matching search criteria
                                </Typography>
                            )}
                        </div>
                    </div>
                    <Footer />
                </div>
            </div>
        );
    }
}
