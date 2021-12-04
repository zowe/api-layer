import { Component, Suspense } from 'react';
import { NavTab } from 'react-router-tabs';
import { IconButton, Typography, Tooltip } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ErrorIcon from '@material-ui/icons/Error';
import { Redirect, Route, Router, Switch } from 'react-router-dom';

import './DetailPage.css';
import './ReactRouterTabs.css';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ServiceTabContainer from '../ServiceTab/ServiceTabContainer';
import PageNotFound from '../PageNotFound/PageNotFound';
import BigShield from '../ErrorBoundary/BigShield/BigShield';

export default class DetailPage extends Component {
    componentDidMount() {
        const { fetchTilesStart, match } = this.props;
        fetchTilesStart(match.params.tileID);
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

    setTitle = (title, status) => {
        if (status === 'DOWN') {
            return `${title} - This service is not running`;
        }
        return title;
    };

    tileId = null;

    render() {
        const {
            tiles,
            isLoading,
            clearService,
            fetchTilesStop,
            fetchTilesError,
            selectedTile,
            match,
            match: {
                params: { tileID },
            },
            fetchTilesStart,
            history,
        } = this.props;
        const iconBack = <ChevronLeftIcon />;
        let error = null;
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        } else if (selectedTile !== null && selectedTile !== undefined && selectedTile !== tileID) {
            clearService();
            fetchTilesStop();
            fetchTilesStart(tileID);
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
                            Tile details for "{match.params.tileID}" could not be retrieved, the following error was
                            returned:
                        </Typography>
                        {error}
                    </div>
                )}
                {!isLoading &&
                    !fetchTilesError && (
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
                                    {tiles !== undefined &&
                                        tiles.length === 1 && (
                                            <div id="title" className="text-block-11">
                                                {tiles[0].title}
                                            </div>
                                        )}
                                </div>
                                <div className="paragraph-description-container">
                                    {tiles !== undefined &&
                                        tiles.length > 0 && (
                                            <div id="description" className="text-block-12">
                                                {tiles[0].description}
                                            </div>
                                        )}
                                </div>
                            </div>
                        </div>
                    )}
                <div className="content-description-container">
                    {tiles !== undefined &&
                        tiles.length === 1 && (
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
                                                    <div className="tabs-container">
                                                        {tiles !== undefined &&
                                                            tiles.length === 1 &&
                                                            tiles[0].services.map(({ serviceId, title, status }) => (
                                                                <Tooltip
                                                                    key={serviceId}
                                                                    title={this.setTitle(title, status)}
                                                                    placement="bottom"
                                                                >
                                                                    <div id="service-tab">
                                                                        {status === 'UP' && (
                                                                            <NavTab to={`${match.url}/${serviceId}`}>
                                                                                <Typography
                                                                                    id="serviceIdTabs"
                                                                                    variant="subtitle2"
                                                                                    style={{
                                                                                        color: 'black',
                                                                                        marginBottom: '12px',
                                                                                    }}
                                                                                >
                                                                                    {serviceId}
                                                                                </Typography>
                                                                            </NavTab>
                                                                        )}
                                                                        {status === 'DOWN' && (
                                                                            <NavTab to={`${match.url}/${serviceId}`}>
                                                                                <Typography
                                                                                    variant="subtitle2"
                                                                                    style={{ color: '#de1b1b' }}
                                                                                >
                                                                                    {serviceId}
                                                                                </Typography>
                                                                                <ErrorIcon
                                                                                    style={{ color: '#de1b1b' }}
                                                                                />
                                                                            </NavTab>
                                                                        )}
                                                                    </div>
                                                                </Tooltip>
                                                            ))}
                                                    </div>
                                                    <ServiceTabContainer />
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
