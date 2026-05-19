/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { Suspense, useEffect, useCallback } from 'react';
import { Container, Divider, IconButton, Link, Typography } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import { Navigate, Routes, Route, useNavigate, useLocation } from 'react-router';
import PropTypes from 'prop-types';
import Footer from '../Footer/Footer';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ServiceTabContainer from '../ServiceTab/ServiceTabContainer';
import PageNotFound from '../PageNotFound/PageNotFound';
import BigShieldWrapper from '../ErrorBoundary/BigShield/BigShieldWrapper';
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

export default function DetailPage(props) {
    const navigate = useNavigate();
    const location = useLocation();

    const {
        isLoading,
        clearService,
        fetchTilesStop,
        fetchTilesError,
        selectedTile,
        services,
        fetchTilesStart,
        currentTileId,
        fetchNewTiles,
        selectedService,
        selectedContentAnchor,
    } = props;
    let { tiles } = props;

    useEffect(() => {
        const elementToView = document.querySelector(selectedContentAnchor);
        if (elementToView) {
            setTimeout(() => {
                elementToView.scrollIntoView({ behavior: 'smooth' });
            }, 300);
        }
    }, [selectedContentAnchor]);

    useEffect(() => {
        if (isAPIPortal()) {
            document.title = process.env.REACT_APP_API_PORTAL_SERVICE_TITLE;
            closeMobileMenu();
            const goBackButton = document.getElementById('go-back-button-portal');
            if (goBackButton) {
                goBackButton.style.removeProperty('display');
            }
        }
        fetchNewTiles();
        if (currentTileId) {
            fetchTilesStart(currentTileId);
        }
        return () => {
            fetchTilesStop();
        };
    }, [fetchTilesStart, currentTileId, fetchNewTiles, fetchTilesStop]);

    const handleGoBack = useCallback(() => {
        navigate('/dashboard');
    }, [navigate]);

    const handleLinkClick = useCallback((e, id) => {
        e.preventDefault();
        const elementToView = document.querySelector(id);
        if (elementToView) {
            elementToView.scrollIntoView({ behavior: 'smooth' });
        }
    }, []);

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
        const id = location.pathname.split('/service/')[1];
        if (id) {
            const correctTile = services.find((tile) => tile.services.some((service) => service.serviceId === id));
            if (correctTile) {
                tiles = [correctTile];
            }
        }
    }

    const apiPortalEnabled = isAPIPortal();
    const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
    const {
        useCasesCounter,
        tutorialsCounter,
        videosCounter,
        filteredUseCases,
        filteredTutorials,
        videos,
        documentation,
    } = countAdditionalContents(selectedService);
    const onlySwaggerPresent = tutorialsCounter === 0 && videosCounter === 0 && useCasesCounter === 0;
    const showSideBar = false;
    if (hasTiles && tiles[0]?.customStyleConfig && Object.keys(tiles[0].customStyleConfig).length > 0) {
        customUIStyle(tiles[0].customStyleConfig);
    }

    const iconBack = <ChevronLeftIcon />;

    return (
        <div className="main">
            {apiPortalEnabled && <FeedbackButton />}
            <div className="nav-bar">
                {services !== undefined && services.length > 0 && (
                    <Shield>
                        <ServicesNavigationBarContainer services={services} />
                    </Shield>
                )}
            </div>

            <div className="main-content2 detail-content">
                {apiPortalEnabled && <Divider light id="footer-divider" />}
                <Spinner isLoading={isLoading} />
                {fetchTilesError && (
                    <div className="no-tiles-container">
                        <br />
                        <IconButton id="go-back-button" onClick={handleGoBack} size="medium">
                            {iconBack}
                            Back
                        </IconButton>
                        <br />
                        <br />
                        <Typography style={{ color: '#de1b1b' }} data-testid="detail-page-error" variant="subtitle2">
                            Tile details for "{currentTileId}" could not be retrieved, the following error was returned:
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
                                onClick={handleGoBack}
                                size="medium"
                            >
                                {iconBack}
                                Back
                            </IconButton>
                        )}
                        <div className="detailed-description-container">
                            <div className="title-api-container">
                                {tiles !== undefined && tiles.length === 1 && (
                                    <h2 id="title" className="text-block-11 title1">
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
                                    <Link className="links" onClick={(e) => handleLinkClick(e, '#swagger-label')}>
                                        Swagger
                                    </Link>
                                    <Link className="links" onClick={(e) => handleLinkClick(e, '#use-cases-label')}>
                                        Use cases ({useCasesCounter})
                                    </Link>
                                    <Link className="links" onClick={(e) => handleLinkClick(e, '#tutorials-label')}>
                                        Getting Started ({tutorialsCounter})
                                    </Link>
                                    <Link className="links" onClick={(e) => handleLinkClick(e, '#videos-label')}>
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
                            <Routes>
                                <Route index element={<Navigate replace to={`${tiles[0].services[0].serviceId}`} />} />
                                <Route
                                    path=":serviceId"
                                    element={
                                        <div className="tabs-swagger">
                                            <ServiceTabContainer
                                                videos={videos}
                                                useCases={filteredUseCases}
                                                tutorials={filteredTutorials}
                                                videosCounter={videosCounter}
                                                tutorialsCounter={tutorialsCounter}
                                                useCasesCounter={useCasesCounter}
                                                documentation={documentation}
                                                tiles={tiles}
                                            />
                                        </div>
                                    }
                                />
                                <Route
                                    element={
                                        <BigShieldWrapper>
                                            <PageNotFound />
                                        </BigShieldWrapper>
                                    }
                                />
                            </Routes>
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

DetailPage.defaultProps = {
    fetchTilesError: null,
    selectedTile: null,
    services: null,
    currentTileId: null,
};

DetailPage.propTypes = {
    selectedService: PropTypes.object.isRequired,
    selectedContentAnchor: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(
        PropTypes.shape({
            title: PropTypes.string.isRequired,
            customStyleConfig: PropTypes.object.isRequired,
        })
    ).isRequired,
    isLoading: PropTypes.bool.isRequired,
    clearService: PropTypes.func.isRequired,
    fetchTilesStop: PropTypes.func.isRequired,
    fetchTilesError: PropTypes.any,
    selectedTile: PropTypes.any,
    services: PropTypes.arrayOf(PropTypes.any),
    fetchTilesStart: PropTypes.func.isRequired,
    currentTileId: PropTypes.any,
    fetchNewTiles: PropTypes.func.isRequired,
};
