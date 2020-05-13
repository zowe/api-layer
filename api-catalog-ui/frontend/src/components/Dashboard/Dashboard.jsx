import { Text, Button, Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions } from 'mineral-ui';
import React, { Component } from 'react';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import './Dashboard.css';
import Tile from '../Tile/Tile';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';

export default class Dashboard extends Component {
    componentDidMount() {
        const { fetchTilesStart, clearService } = this.props;
        clearService();
        fetchTilesStart();
    }

    componentWillUnmount() {
        const { fetchTilesStop, clear } = this.props;
        clear();
        fetchTilesStop();
    }

    handleSearch = value => {
        const { filterText } = this.props;
        filterText(value);
    };

    refreshStaticApis = () => {
        const { refreshedStaticApi } = this.props;
        refreshedStaticApi();
    }

    handleClose = () => {
        // TODO
    }

    formatTimestamp = () => {
        const { refreshTimestamp } = this.props;
        let formattedTime;
        if (refreshTimestamp !== undefined && refreshTimestamp !== null) {
            formattedTime = new Date(refreshTimestamp).toString();
        }
        return formattedTime;
    }

    getCorrectRefreshMessage = error => {
        let refreshError = "OPS! Something went wrong! Unable to refresh the static APIs.";
        if (error && error.status) {
            if (error.status === 500) {
                refreshError = "500 - Internal Server Error: Something went wrong and the static API refresh could not be performed";
            }
            else if (error.status === 503) {
                refreshError = "503 - Service Unavailable: The service could not be found. Check if the service is up and registered";
            }
        }
        return refreshError;
    }

    render() {
        const { tiles, history, searchCriteria, isLoading, fetchTilesError, fetchTilesStop, refreshedStaticApisError } = this.props;
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        let date = this.formatTimestamp();
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        let refreshError = this.getCorrectRefreshMessage(refreshedStaticApisError);
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        return (
            <div>
                <Spinner isLoading={isLoading} />
                {fetchTilesError && (
                    <div className="no-tiles-container">
                        <br />
                        <br />
                        <Text element="h3">Tile details could not be retrieved, the following error was returned:</Text>
                        {error}
                    </div>
                )}
                {refreshedStaticApisError !== null &&
                refreshedStaticApisError !== undefined &&
                refreshedStaticApisError.status
                && (
                        <Dialog
                            variant="danger"
                            appSelector="#App"
                            closeOnClickOutside="true"
                            hideOverlay="true"
                            isOpen={refreshedStaticApisError}
                        >
                            <DialogHeader>
                                <DialogTitle>Error</DialogTitle>
                            </DialogHeader>
                            <DialogBody>
                                <Text>{refreshError}</Text>
                            </DialogBody>
                            <DialogFooter>
                                <DialogActions>
                                    <Button size="medium" variant="danger" onClick={this.handleClose}>
                                        Close
                                    </Button>
                                </DialogActions>
                            </DialogFooter>
                        </Dialog>
                )
                }
                {date !== undefined && date !== null && (
                        <Text id="timestamp" element="h6">The last static APIs refresh was done on {date}</Text>
                )}
                {!fetchTilesError && (
                    <div className="apis">
                        <div className="grid-container">
                            <div className="filtering-container">
                                <Shield title="Search Bar is broken !">
                                    <SearchCriteria placeholder="Search for APIs" doSearch={this.handleSearch} />
                                </Shield>
                                <h2 className="api-heading">Available API services</h2>
                                <div>
                                    <Button size="medium" onClick={this.refreshStaticApis}>Refresh Static APIs</Button>
                                </div>
                            </div>
                            {hasTiles && tiles.map(tile => <Tile key={tile.id} tile={tile} history={history} />)}
                            {!hasTiles &&
                                hasSearchCriteria && (
                                    <Text id="search_no_results" element="h4" color="#1d5bbf">
                                        No tiles found matching search criteria
                                    </Text>
                                )}
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
