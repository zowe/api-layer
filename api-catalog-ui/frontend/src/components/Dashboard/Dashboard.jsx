/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Typography, IconButton, Snackbar, Container, Link } from '@material-ui/core';
import { Alert } from '@mui/material';
import { Component } from 'react';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import './Dashboard.css';
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
        const hasSearchCriteria = searchCriteria !== undefined && searchCriteria !== null && searchCriteria.length > 0;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        let error = null;
        if (fetchTilesError !== undefined && fetchTilesError !== null) {
            fetchTilesStop();
            error = formatError(fetchTilesError);
        }

        return (
            <div>
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
                        <div id="grid-container">
                            <div className="api-heading-container">
                                <h3 id="api-heading-id" className="api-heading">
                                    API Developer Experience
                                </h3>
                                <div>
                                    <hr id="separator" />
                                </div>
                            </div>
                            <div id="search">
                                <Shield title="Search Bar is broken !">
                                    <SearchCriteria
                                        id="search-input"
                                        placeholder="Search for APIs..."
                                        doSearch={this.handleSearch}
                                    />
                                </Shield>
                            </div>
                            <div id="info-headers-div">
                                <h3 className="info-headers">Swagger</h3>
                                <h3 className="info-headers">Use Cases</h3>
                                <h3 className="info-headers">Tutorials</h3>
                                <h3 className="info-headers">Videos</h3>
                            </div>
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
                                <Typography id="search_no_results" variant="subtitle2" style={{ color: '#1d5bbf' }}>
                                    No services found matching search criteria
                                </Typography>
                            )}
                        </div>
                        <div id="bottom-info-div">
                            <Container>
                                <h4 className="footer-links">Capabilities</h4>
                                <Link className="links" />
                            </Container>
                            <vl id="footer-menu-separator" />
                            <Container>
                                <h4>Resources</h4>
                                <Link className="links">Blog</Link>
                            </Container>
                            <vl id="footer-menu-separator" />
                            <Container>
                                <h4>Just a placeholder</h4>
                                <Link className="links" />
                            </Container>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
