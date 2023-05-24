/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component, Suspense } from 'react';
import { IconButton, Typography } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import { Redirect, Route, Router, Switch } from 'react-router-dom';

import './DetailPage.css';
import './ReactRouterTabs.css';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ServiceTabContainer from '../ServiceTab/ServiceTabContainer';
import PageNotFound from '../PageNotFound/PageNotFound';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ServicesNavigationBarContainer from '../ServicesNavigationBar/ServicesNavigationBarContainer';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class DetailPage extends Component {
    componentDidMount() {
        const { fetchTilesStart, currentTileId } = this.props;
        fetchTilesStart(currentTileId);
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
    //
    // setTitle = (title, status) => {
    //     if (status === 'DOWN') {
    //         return `${title} - This service is not running`;
    //     }
    //     return title;
    // };

    render() {
        const {
            tiles,
            isLoading,
            clearService,
            fetchTilesStop,
            fetchTilesError,
            selectedTile,
            originalTiles,
            match,
            fetchTilesStart,
            history,
            currentTileId,
        } = this.props;
        const iconBack = <ChevronLeftIcon />;
        let error = null;
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        } else if (selectedTile !== null && selectedTile !== undefined && selectedTile !== currentTileId) {
            clearService();
            fetchTilesStop();
            fetchTilesStart(currentTileId);
        }
        return (
            <div className="detail-page">
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
                        <br />
                        <br />
                        <Typography style={{ color: '#de1b1b' }} data-testid="detail-page-error" variant="subtitle2">
                            Tile details for "{currentTileId}" could not be retrieved, the following error was returned:
                        </Typography>
                        {error}
                    </div>
                )}
                <div className="nav-bar">
                    {originalTiles !== undefined && originalTiles.length > 0 && (
                        <Shield>
                            <ServicesNavigationBarContainer originalTiles={originalTiles} match={match} />
                        </Shield>
                    )}
                </div>
                <div className="content-description-container">
                    {tiles !== undefined && tiles.length === 1 && (
                        <Suspense>
                            <Router history={history}>
                                <Switch>
                                    <Route
                                        exact
                                        path={`${match.path}`}
                                        render={() => (
                                            <Redirect replace to={`${match.url}/${tiles[0].services[0].serviceId}`} />
                                        )}
                                    />
                                    <Route
                                        exact
                                        path={`${match.path}/:serviceId`}
                                        render={() => (
                                            <div>
                                                {!isLoading && !fetchTilesError && (
                                                    <div className="api-description-container">
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
                                                        <div className="detailed-description-container">
                                                            <div className="title-api-container">
                                                                {tiles !== undefined && tiles.length === 1 && (
                                                                    <div id="title" className="text-block-11">
                                                                        {tiles[0].title}
                                                                    </div>
                                                                )}
                                                            </div>
                                                            <div className="paragraph-description-container">
                                                                {tiles !== undefined && tiles.length > 0 && (
                                                                    <div id="description" className="text-block-12">
                                                                        {tiles[0].description}
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </div>
                                                )}
                                                <div className="tabs-swagger">
                                                    <ServiceTabContainer />
                                                </div>
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
                </div>
            </div>
        );
    }
}
