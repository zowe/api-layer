/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Typography, IconButton, Snackbar } from '@material-ui/core';
import { Alert } from '@mui/material';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Footer from '../Footer/Footer';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import Tile from '../Tile/Tile';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ErrorDialog from '../Error/ErrorDialog';
import WizardContainer from '../Wizard/WizardContainer';
import DialogDropdown from '../Wizard/DialogDropdown';
import { enablerData } from '../Wizard/configs/wizard_onboarding_methods';
import ConfirmDialogContainer from '../Wizard/ConfirmDialogContainer';
import { customUIStyle } from '../../utils/utilFunctions';
import { sortServices } from '../../selectors/selectors';

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

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    refreshStaticApis = () => {
        const { refreshedStaticApi } = this.props;
        refreshedStaticApi();
    };

    toggleWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    handleClose = () => {
        const { closeAlert } = this.props;
        closeAlert();
    };

    render() {
        const {
            tiles,
            history,
            searchCriteria,
            isLoading,
            fetchTilesError,
            fetchTilesStop,
            refreshedStaticApisError,
            clearError,
            authentication,
            storeCurrentTileId,
            storeContentAnchor,
        } = this.props;
        const hasSearchCriteria =
            typeof searchCriteria !== 'undefined' &&
            searchCriteria !== undefined &&
            searchCriteria !== null &&
            searchCriteria.length > 0;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        if (hasTiles && 'customStyleConfig' in tiles[0] && tiles[0].customStyleConfig) {
            customUIStyle(tiles[0].customStyleConfig);
        }
        let allServices;
        if (hasTiles) {
            allServices = sortServices(tiles);
        }

        return (
            <div className="main-content dashboard-content">
                <div id="dash-buttons">
                    <DialogDropdown
                        selectEnabler={this.props.selectEnabler}
                        data={enablerData}
                        toggleWizard={this.toggleWizard}
                        visible
                    />
                    <IconButton
                        id="refresh-api-button"
                        size="medium"
                        variant="outlined"
                        onClick={this.refreshStaticApis}
                        style={{ borderRadius: '0.1875em' }}
                    >
                        Refresh Static APIs
                    </IconButton>
                </div>
                <WizardContainer />
                <Snackbar
                    anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                    open={authentication.showUpdatePassSuccess}
                    onClose={this.handleClose}
                >
                    <Alert onClose={this.handleClose} severity="success" sx={{ width: '100%' }}>
                        Your mainframe password was successfully changed.
                    </Alert>
                </Snackbar>
                <ConfirmDialogContainer />
                <Spinner isLoading={isLoading} />
                {fetchTilesError && (
                    <div className="no-tiles-container">
                        <br />
                        <br />
                        <Typography data-testid="error" variant="subtitle1">
                            Tile details could not be retrieved, the following error was returned:
                        </Typography>
                        {error}
                    </div>
                )}
                <ErrorDialog refreshedStaticApisError={refreshedStaticApisError} clearError={clearError} />
                {!fetchTilesError && (
                    <div className="apis">
                        <div
                            id="grid-container"
                            onScroll={(e) => {
                                this.dashboardTileScroll(e);
                            }}
                        >
                            <div className="filtering-container">
                                <div id="search">
                                    <Shield title="Search Bar is broken !">
                                        <SearchCriteria
                                            id="search-input"
                                            placeholder="Search..."
                                            doSearch={this.handleSearch}
                                        />
                                    </Shield>
                                </div>
                            </div>

                            <div className="tile-container">
                                {isLoading && <div className="loadingDiv" />}

                                {hasTiles &&
                                    allServices.map((service) =>
                                        tiles
                                            .filter((tile) => tile.services.includes(service))
                                            .map((tile) => (
                                                <Tile
                                                    storeCurrentTileId={storeCurrentTileId}
                                                    storeContentAnchor={storeContentAnchor}
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
                                {hasTiles && (
                                    <div id="dashboardFooter">
                                        <Footer />
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}

Dashboard.propTypes = {
    tiles: PropTypes.shape({
        filter: PropTypes.func.isRequired,
    }).isRequired,
    storeContentAnchor: PropTypes.func.isRequired,
};
