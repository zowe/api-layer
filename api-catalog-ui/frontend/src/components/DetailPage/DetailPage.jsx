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
                        selectedTile,
                        services,
                        fetchTilesStart,
                        currentTileId,
                        fetchNewTiles,
                        tiles,
                        authentication,
                        selectService,
                        storeCurrentTileId,
                        fetchNewService,
                        service,
                        fetchServiceStop,
                        tilesLoading,
                        serviceLoading

                    }) {
    const [error, setError] = useState(null);
    const serviceId = useParams();



    useEffect(() => {

        console.log(currentTileId)
        // fetchTilesStart();
        console.log(serviceId['*'])
        // if(service.serviceId != serviceId['*']){
        fetchNewService(serviceId['*']);
        // }
        console.log(service)
        console.log(tiles)

        if (fetchTilesError) {
            fetchServiceStop();
            setError(formatError(fetchTilesError));
        }
        // return () => {
        //     fetchTilesStop();
        //     fetchServiceStop();
        // };
    }, [fetchTilesStart, fetchTilesStop, fetchTilesError,fetchServiceStop,fetchNewService]);


    const navigate = useNavigate();
    const handleGoBack = () => {
        navigate('/dashboard');
    };
    const iconBack = <ChevronLeftIcon/>;
    const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
    if (hasTiles && tiles[0]?.customStyleConfig && Object.keys(tiles[0].customStyleConfig).length > 0) {
        customUIStyle(tiles[0].customStyleConfig);
    }
    if (authentication?.error?.status === 401) {
        navigate('/login');
    }

    return (
        <div className="main">
            <div className="nav-bar">
                {tiles !== undefined && tiles.length > 0 && (
                    <Shield>
                        <ServicesNavigationBarContainer/>
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
                    {!serviceLoading && service && (
                        <Suspense>
                            <div>
                                <Routes>
                                    <Route
                                        exact
                                        path={`/`}
                                        element={
                                            <Navigate
                                                replace={true}
                                                to={`/gateway`}
                                            />
                                        }
                                    />
                                    <Route
                                        path=":serviceId"
                                        element={
                                            <div className="tabs-swagger">

                                                <ServiceTabContainer/>
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

