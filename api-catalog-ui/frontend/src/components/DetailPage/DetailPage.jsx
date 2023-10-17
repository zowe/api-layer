/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { Component, Suspense } from 'react';
import { Container, Divider, IconButton, Link, Typography } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import PropTypes from 'prop-types';
import Footer from '../Footer/Footer';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ServiceTabContainer from '../ServiceTab/ServiceTabContainer';
import PageNotFound from '../PageNotFound/PageNotFound';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ServicesNavigationBarContainer from '../ServicesNavigationBar/ServicesNavigationBarContainer';
import Shield from '../ErrorBoundary/Shield/Shield';
import countAdditionalContents, { customUIStyle, isAPIPortal, closeMobileMenu } from '../../utils/utilFunctions';

const loadFeedbackButton = () => {
    if (isAPIPortal()) {
        return import('../FeedbackButton/FeedbackButton');
    }
    return Promise.resolve(null);
};

const FeedbackButton = React.lazy(loadFeedbackButton);

export default class DetailPage extends Component {
    componentDidMount() {
        if (isAPIPortal()) {
            closeMobileMenu();
            const goBackButton = document.getElementById('go-back-button-portal');
            if (goBackButton) {
                goBackButton.style.removeProperty('display');
            }
        }
        const { fetchTilesStart, currentTileId, fetchNewTiles, history } = this.props;
        fetchNewTiles();
        if (currentTileId) {
            fetchTilesStart(currentTileId);
        }
        if (!localStorage.getItem('serviceId')) {
            const id = history.location.pathname.split('/service/')[1];
            localStorage.setItem('serviceId', id);
        }
        localStorage.removeItem('selectedTab');
    }

    componentWillUnmount() {
        const { fetchTilesStop } = this.props;
        fetchTilesStop();
    }

    // eslint-disable-next-line react/sort-comp
    handleGoBack = () => {
        const { history } = this.props;
        history.push('/dashboard');
    };

    handleLinkClick = (e, id) => {
        e.preventDefault();
        const elementToView = document.querySelector(id);
        if (elementToView) {
            elementToView.scrollIntoView();
        }
    };

    render() {
        const {
            isLoading,
            clearService,
            fetchTilesStop,
            fetchTilesError,
            selectedTile,
            services,
            match,
            fetchTilesStart,
            history,
            currentTileId,
            fetchNewTiles,
        } = this.props;
        let { tiles } = this.props;
        const iconBack = <ChevronLeftIcon />;
        let error = null;
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        } else if (selectedTile !== null && selectedTile !== undefined && selectedTile !== currentTileId) {
            clearService();
            fetchTilesStop();
            fetchNewTiles();
            fetchTilesStart(currentTileId);
        } else if (services && services.length > 0 && !currentTileId) {
            const id = history.location.pathname.split('/service/')[1];
            if (id) {
                const correctTile = services.find((tile) => tile.services.some((service) => service.serviceId === id));
                if (correctTile) {
                    tiles = [correctTile];
                }
            }
        }
        const apiPortalEnabled = isAPIPortal();
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        const { useCasesCounter, tutorialsCounter, videosCounter } = countAdditionalContents(services);
        const onlySwaggerPresent = tutorialsCounter === 0 && videosCounter === 0 && useCasesCounter === 0;
        const showSideBar = false;
        if (
            hasTiles &&
            'customStyleConfig' in tiles[0] &&
            tiles[0].customStyleConfig &&
            Object.keys(tiles[0].customStyleConfig).length > 0
        ) {
            customUIStyle(tiles[0].customStyleConfig);
        }
        return (
            <div className="main">
                {apiPortalEnabled && <FeedbackButton />}
                <div className="nav-bar">
                    {services !== undefined && services.length > 0 && (
                        <Shield>
                            <ServicesNavigationBarContainer services={services} match={match} />
                        </Shield>
                    )}
                </div>

                <div className="main-content2 detail-content">
                    {apiPortalEnabled && <Divider light id="footer-divider" />}
                    <Spinner isLoading={isLoading} />
                    {fetchTilesError && (
                        <div className="no-tiles-container">
                            <br />
                            <IconButton id="go-back-button" onClick={this.handleGoBack} size="medium">
                                {iconBack}
                                Back
                            </IconButton>
                            <br />
                            <br />
                            <Typography
                                style={{ color: '#de1b1b' }}
                                data-testid="detail-page-error"
                                variant="subtitle2"
                            >
                                Tile details for "{currentTileId}" could not be retrieved, the following error was
                                returned:
                            </Typography>
                            {error}
                        </div>
                    )}

                    {!isLoading && !fetchTilesError && (
                        <div className="api-description-container">
                            {!apiPortalEnabled && (
                                <IconButton
                                    id="go-back-button"
                                    data-testid="go-back-button"
                                    color="primary"
                                    onClick={this.handleGoBack}
                                    size="medium"
                                >
                                    {iconBack}
                                    Back
                                </IconButton>
                            )}
                            <div className="detailed-description-container">
                                <div className="title-api-container">
                                    {tiles !== undefined && tiles.length === 1 && (
                                        <h2 id="title" className="text-block-11">
                                            {tiles[0].title}
                                        </h2>
                                    )}
                                </div>
                                {!apiPortalEnabled && (
                                    <div className="paragraph-description-container">
                                        {tiles !== undefined && tiles.length > 0 && (
                                            <p id="description" className="text-block-12">
                                                {tiles[0].description}
                                            </p>
                                        )}
                                    </div>
                                )}
                            </div>
                            {apiPortalEnabled && !onlySwaggerPresent && (
                                <div id="right-resources-menu">
                                    <Typography id="resources-menu-title" variant="subtitle1">
                                        On this page
                                    </Typography>
                                    <Container>
                                        <Link
                                            className="links"
                                            onClick={(e) => this.handleLinkClick(e, '#swagger-label')}
                                        >
                                            Swagger
                                        </Link>
                                        <Link
                                            className="links"
                                            onClick={(e) => this.handleLinkClick(e, '#use-cases-label')}
                                        >
                                            Use cases ({useCasesCounter})
                                        </Link>
                                        <Link
                                            className="links"
                                            onClick={(e) => this.handleLinkClick(e, '#tutorials-label')}
                                        >
                                            Tutorials ({tutorialsCounter})
                                        </Link>
                                        <Link
                                            className="links"
                                            onClick={(e) => this.handleLinkClick(e, '#videos-label')}
                                        >
                                            Videos ({videosCounter})
                                        </Link>
                                    </Container>
                                </div>
                            )}
                        </div>
                    )}
                    <div className="content-description-container">
                        {tiles !== undefined && tiles.length === 1 && (
                            <Suspense>
                                <Router history={history}>
                                    <Switch>
                                        <Route
                                            exact
                                            path={`${match.path}`}
                                            render={() => (
                                                <Redirect
                                                    replace
                                                    to={`${match.url}/${tiles[0].services[0].serviceId}`}
                                                />
                                            )}
                                        />
                                        <Route
                                            exact
                                            path={`${match.path}/:serviceId`}
                                            render={() => (
                                                <div className="tabs-swagger">
                                                    <ServiceTabContainer
                                                        videosCounter={videosCounter}
                                                        tutorialsCounter={tutorialsCounter}
                                                        useCasesCounter={useCasesCounter}
                                                        tiles={tiles}
                                                    />
                                                </div>
                                            )}
                                        />
                                        <Route
                                            render={(props, state) => (
                                                <BigShield history={history}>
                                                    <PageNotFound {...props} {...state} />
                                                </BigShield>
                                            )}
                                        />
                                    </Switch>
                                </Router>
                            </Suspense>
                        )}
                        {apiPortalEnabled && <Divider light id="footer-divider" />}
                    </div>

                    <Footer />
                </div>

                {showSideBar && <div className="side-bar" />}
            </div>
        );
    }
}

DetailPage.propTypes = {
    history: PropTypes.shape({
        push: PropTypes.func.isRequired,
    }).isRequired,
};
