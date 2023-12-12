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
import { customUIStyle, isAPIPortal } from '../../utils/utilFunctions';

const loadFeedbackButton = () => {
    if (isAPIPortal()) {
        return import('../FeedbackButton/FeedbackButton');
    }
    return Promise.resolve(null);
};

const FeedbackButton = React.lazy(loadFeedbackButton);

export default class Dashboard extends Component {
    componentDidMount() {
        if (isAPIPortal()) {
            const goBackButton = document.getElementById('go-back-button-portal');
            if (goBackButton) {
                goBackButton.style.display = 'none';
            }
        }
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
        } = this.props;
        const hasSearchCriteria =
            typeof searchCriteria !== 'undefined' &&
            searchCriteria !== undefined &&
            searchCriteria !== null &&
            searchCriteria.length > 0;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        const apiPortalEnabled = isAPIPortal();
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        if (hasTiles && 'customStyleConfig' in tiles[0] && tiles[0].customStyleConfig) {
            customUIStyle(tiles[0].customStyleConfig);
        }
        const dashboardTileScroll = (e) => {
            const getHeader = document.querySelectorAll('.dashboard-grid-header')[0];
            const getHeaderHeight = getHeader?.offsetHeight;
            const getFilterHeight = document.querySelectorAll('.filtering-container')[0]?.offsetHeight;

            if (e.target.scrollTop > getFilterHeight) {
                e.target.classList.add('fixed-header');
                e.target.style.paddingTop = `${
                    getHeaderHeight + getHeader.style.marginBottom + getHeader.style.marginTop
                }px`;
            } else {
                e.target.classList.remove('fixed-header');
                e.target.style.paddingTop = 0;
            }
        };

        return (
            <div className="main-content dashboard-content">
                {isAPIPortal() && <FeedbackButton />}
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
                        <div
                            id="grid-container"
                            onScroll={(e) => {
                                dashboardTileScroll(e);
                            }}
                        >
                            <div className="filtering-container">
                                {apiPortalEnabled && (
                                    <div>
                                        <h1 className="api-heading">API Catalog</h1>
                                    </div>
                                )}
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
                            {apiPortalEnabled && (
                                <div className="dashboard-grid-header">
                                    <div className="empty" />
                                    <h4 className="description-header">Swagger</h4>
                                    <h4 className="description-header">Use Cases</h4>
                                    <h4 className="description-header">Tutorials</h4>
                                    <h4 className="description-header">Videos</h4>
                                </div>
                            )}

                            <div className="tile-container">
                                {isLoading && <div className="loadingDiv" />}

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
                                    <Typography id="search_no_results" variant="subtitle2" className="no-content">
                                        No services found matching search criteria
                                    </Typography>
                                )}
                                <Footer />
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
