/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, {Component, Suspense, useEffect, useRef, useState} from 'react';
import {IconButton, Typography} from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import {Navigate, Route, Router, Routes, useMatch, useNavigate, useParams} from 'react-router';
import PropTypes from 'prop-types';
import Footer from '../Footer/Footer';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ServiceTabContainer from '../ServiceTab/ServiceTabContainer';
import PageNotFound from '../PageNotFound/PageNotFound';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ServicesNavigationBarContainer from '../ServicesNavigationBar/ServicesNavigationBarContainer';
import Shield from '../ErrorBoundary/Shield/Shield';
import {customUIStyle} from '../../utils/utilFunctions';

function DetailPage({
                        isLoading,
                        clearService,
                        fetchTilesStop,
                        fetchTilesError,
                        selectedContentAnchor,
                        selectedTile,
                        services,
                        fetchTilesStart,
                        currentTileId,
                        fetchNewTiles,
                        tiles,
                    }) {
    const [error, setError] = useState(null);
    console.log(selectedContentAnchor);
    useEffect(() => {
        const elementToView = document.querySelector(selectedContentAnchor);
        if (elementToView) {
            setTimeout(() => {
                elementToView.scrollIntoView({behavior: 'smooth'});
            }, 300);
        }
    }, [selectedContentAnchor]);

    useEffect(() => {
        fetchNewTiles();
        if (currentTileId) {
            fetchTilesStart(currentTileId);
        }

        return () => {
            fetchTilesStop();
        };
    }, [fetchNewTiles, fetchTilesStart, fetchTilesStop, currentTileId]);

    useEffect(() => {
        if (fetchTilesError) {
            fetchTilesStop();
            setError(formatError(fetchTilesError));
        } else if (selectedTile && selectedTile !== currentTileId) {
            clearService();
            fetchTilesStop();
            fetchNewTiles();
            fetchTilesStart(currentTileId);
        } else if (services && services.length > 0 && !currentTileId) {
            // const id = history.location.pathname.split('/service/')[1];
            const id = 'apicatalog';
            if (id) {
                const correctTile = services.find((tile) =>
                    tile.services.some((service) => service.serviceId === id)
                );
                if (correctTile) {
                    tiles = [correctTile];
                }
            }
        }
    }, [
        fetchTilesError,
        selectedTile,
        currentTileId,
        clearService,
        fetchTilesStop,
        fetchNewTiles,
        fetchTilesStart,
        services,
    ]);

    const navigate = useNavigate();
    const handleGoBack = () => {

        navigate('/dashboard');
    };
    const iconBack = <ChevronLeftIcon/>;
    const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
    if (hasTiles && tiles[0]?.customStyleConfig && Object.keys(tiles[0].customStyleConfig).length > 0) {
        customUIStyle(tiles[0].customStyleConfig);
    }
    return (
        <div className="main">
            <div className="nav-bar">
                {services !== undefined && services.length > 0 && (
                    <Shield>
                        <ServicesNavigationBarContainer services={services}/>
                    </Shield>
                )}
            </div>

            <div className="main-content2 detail-content">
                <Spinner isLoading={isLoading}/>
                {fetchTilesError && (
                    <div className="no-tiles-container">
                        <br/>
                        <IconButton id="go-back-button" onClick={handleGoBack} size="medium">
                            {iconBack}
                            Back
                        </IconButton>
                        <br/>
                        <br/>
                        <Typography
                            style={{color: '#de1b1b'}}
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
                        <div className="detailed-description-container">
                            <div className="title-api-container">
                                {tiles !== undefined && tiles.length === 1 && (
                                    <h2 id="title" className="text-block-11 title1">
                                        {tiles[0].title}
                                    </h2>
                                )}
                            </div>
                            <div className="paragraph-description-container">
                                {tiles !== undefined && tiles.length > 0 && (
                                    <p id="description" className="text-block-12">
                                        {tiles[0].description}
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>
                )}
                <div className="content-description-container">
                    {tiles !== undefined && tiles.length === 1 && (
                        <Suspense>
                            <div>
                                <Routes>
                                    <Route
                                        exact
                                        path={`/`}
                                        element={
                                            <Navigate
                                                replace={true}
                                                to={`/${tiles[0].services[0].serviceId}`}
                                            />
                                        }
                                    />
                                    <Route
                                        path=":serviceId"
                                        element={
                                            <div className="tabs-swagger">

                                                <ServiceTabContainer tiles={tiles}/>
                                            </div>
                                        }
                                    />
                                    <Route
                                        element={
                                            <BigShield>
                                                <PageNotFound/>
                                            </BigShield>
                                        }
                                    />
                                </Routes>
                                <div id="detailFooter">
                                    <Footer/>
                                </div>
                            </div>
                        </Suspense>
                    )}
                </div>
            </div>
        </div>
    );

}

export default DetailPage;

// DetailPage.propTypes = {
//     history: PropTypes.shape({
//         push: PropTypes.func.isRequired,
//     }).isRequired,
//     selectedService: PropTypes.object.isRequired,
//     selectedContentAnchor: PropTypes.string.isRequired,
//     tiles: PropTypes.arrayOf(
//         PropTypes.shape({
//             title: PropTypes.string.isRequired,
//             customStyleConfig: PropTypes.object.isRequired,
//         })
//     ).isRequired,
// };
