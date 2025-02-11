/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { Suspense, useEffect, useState} from 'react';
import {IconButton, Typography} from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import {Navigate, Route, Routes, useNavigate, useParams} from 'react-router';
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
                        fetchTilesError,
                        tiles,
                        authentication,
                        fetchNewService,
                        service,
                        fetchServiceStop,
                        serviceLoading,
                        fetchServiceError
                    }) {
    const [error, setError] = useState(null);
    const serviceId = useParams();

    useEffect(() => {
        if (!serviceId || !serviceId['*']) {
            console.error("No valid serviceId found:", serviceId);
            return;
        }

        console.log("Fetching service with ID:", serviceId['*']);
        fetchNewService(serviceId['*']);

        if (fetchServiceError) {
            console.warn("Service fetch error:", fetchServiceError);
            fetchServiceStop();
            setError(formatError(fetchServiceError));
        }

        return () => {
            console.log("Cleaning up service fetch");
            fetchServiceStop();
        };
    }, [serviceId['*']]);

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
                <Spinner isLoading={serviceLoading}/>
                {fetchServiceError && (
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
                            Details for service "{serviceId['*']}" could not be retrieved, the following error was
                            returned:
                        </Typography>
                        {error}
                    </div>
                )}

                {!serviceLoading && !fetchServiceError && (
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
                                {service !== undefined && (
                                    <h2 id="title" className="text-block-11 title1">
                                        {service.title}
                                    </h2>
                                )}
                            </div>
                            <div className="paragraph-description-container">
                                {service !== undefined && (
                                    <p id="description" className="text-block-12">
                                        {service.tileDescription}
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

