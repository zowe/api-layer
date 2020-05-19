import { Text, Button, Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions } from 'mineral-ui';
import React, { Component } from 'react';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import './Dashboard.css';
import Tile from '../Tile/Tile';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';

export default class Dashboard extends Component {

    closeDialog = () => {
        const { clearError } = this.props;
        clearError();
    };

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
    };

    getCorrectRefreshMessage = error => {
        let messageText;
        const errorMessages = require("../../error-messages.json");
        if (error && error.status) {
            messageText = "Unexpected error, please try again later";
            if (error.status === 500) {
                messageText = `(${errorMessages.messages[1].messageKey}) ${errorMessages.messages[6].messageText}`;
            }
            else if (error.status === 503) {
                messageText = `(${errorMessages.messages[1].messageKey}) ${errorMessages.messages[5].messageText}`;
            }
        }
        return messageText;
    };

    render() {
        const { tiles, history, searchCriteria, isLoading, fetchTilesError, fetchTilesStop, refreshedStaticApisError } = this.props;
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        const isTrue = true;
        const isFalse = false;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        let refreshError = this.getCorrectRefreshMessage(refreshedStaticApisError);
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        return (
            <div>
                <Button id="refresh-api-button"size="medium" onClick={this.refreshStaticApis}>Refresh Static APIs</Button>
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
                            closeOnClickOutside={isFalse}
                            hideOverlay={isTrue}
                            modeless={isFalse}
                            isOpen={refreshedStaticApisError!==null}
                        >
                            <DialogHeader>
                                <DialogTitle>Error</DialogTitle>
                            </DialogHeader>
                            <DialogBody>
                                <Text>{refreshError}</Text>
                            </DialogBody>
                            <DialogFooter>
                                <DialogActions>
                                    <Button size="medium" variant="danger" onClick={this.closeDialog}>
                                        Close
                                    </Button>
                                </DialogActions>
                            </DialogFooter>
                        </Dialog>
                )
                }

                {!fetchTilesError && (
                    <div className="apis">
                        <div className="grid-container">
                            <div className="filtering-container">
                                <Shield title="Search Bar is broken !">
                                    <SearchCriteria placeholder="Search for APIs" doSearch={this.handleSearch} />
                                </Shield>
                                <h2 className="api-heading">Available API services</h2>
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
