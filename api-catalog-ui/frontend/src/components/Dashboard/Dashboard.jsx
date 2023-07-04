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
import { Component } from 'react';
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

    /**
     * Custom the UI look to match the setup from the service metadata
     * @param tiles
     */
    // eslint-disable-next-line react/no-unused-class-component-methods
    customUIStyle = (tiles) => {
        const root = document.documentElement;
        const logo = document.getElementById('logo');
        if (logo) {
            logo.src = '';
        }
        // eslint-disable-next-line no-console
        console.log(logo);
        if (tiles[0].backgroundColor) {
            root?.style.setProperty('--surface00', tiles[0].backgroundColor);
        }
        if (tiles[0].headerColor) {
            const divider = document.getElementById('separator2');
            const logoutButton = document.getElementById('logout-button');
            const titleLabel = document.getElementById('title');
            const swaggerLabel = document.getElementById('title');
            const header = document.getElementsByClassName('header');
            const spinner = document.getElementsByClassName('lds-ring');
            if (header && header.length > 0) {
                header[0].style.setProperty('background-color', tiles[0].headerColor);
            }
            if (spinner && spinner.length > 0) {
                spinner[0].style.setProperty('border-color', tiles[0].headerColor);
            }
            if (divider) {
                divider.style.setProperty('background-color', tiles[0].headerColor);
            }
            if (logoutButton) {
                logoutButton.style.setProperty('background-color', tiles[0].headerColor);
            }
            if (titleLabel) {
                titleLabel.style.setProperty('color', tiles[0].headerColor);
            }
            if (swaggerLabel) {
                swaggerLabel.style.setProperty('color', tiles[0].headerColor);
            }
        }
        if (tiles[0].fontFamily) {
            root?.style.setProperty('--fontFamily00', tiles[0].fontFamily);
        }
        if (tiles[0].hyperlinksColor) {
            root?.style.setProperty('--link10Hover', tiles[0].hyperlinksColor);
            root?.style.setProperty('--controlText15', tiles[0].hyperlinksColor);
            root?.style.setProperty('--criticalShade10', tiles[0].hyperlinksColor);
            root?.style.setProperty('--criticalShade00', tiles[0].hyperlinksColor);
        }
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
        } = this.props;
        const hasSearchCriteria =
            typeof searchCriteria !== 'undefined' &&
            searchCriteria !== undefined &&
            searchCriteria !== null &&
            searchCriteria.length > 0;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        const apiPortalEnabled =
            process.env.REACT_APP_API_PORTAL !== undefined && process.env.REACT_APP_API_PORTAL === 'true';
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        // if (hasTiles && 'customStyleConfig' in tiles[0] && tiles[0].customStyleConfig) {
        //     this.customUIStyle(tiles);
        // }
        return (
            <div className="main-content dashboard-content">
                {!apiPortalEnabled && (
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
                )}
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
                        <div id="grid-container">
                            <div className="filtering-container">
                                {apiPortalEnabled && (
                                    <div>
                                        <h1 className="api-heading">API Catalog</h1>
                                        <h3>Discover All Broadcom APIs in one place</h3>
                                    </div>
                                )}
                                <div id="search">
                                    <Shield title="Search Bar is broken !">
                                        <SearchCriteria
                                            id="search-input"
                                            placeholder="Search for APIs..."
                                            doSearch={this.handleSearch}
                                        />
                                    </Shield>
                                </div>
                            </div>
                            {apiPortalEnabled && (
                                <div className="dashboard-grid-header">
                                    <h4 className="description-header">Use Cases</h4>
                                    <h4 className="description-header">Tutorials</h4>
                                    <h4 className="description-header">Videos</h4>
                                </div>
                            )}
                            <hr id="separator2" />
                            {hasTiles &&
                                tiles.map((tile) =>
                                    tile.services.map((service) => (
                                        <Tile
                                            storeCurrentTileId={storeCurrentTileId}
                                            service={service}
                                            key={service}
                                            tile={tile}
                                            history={history}
                                        />
                                    ))
                                )}
                            {!hasTiles && hasSearchCriteria && (
                                <Typography id="search_no_results" variant="subtitle2">
                                    No services found matching search criteria
                                </Typography>
                            )}
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
