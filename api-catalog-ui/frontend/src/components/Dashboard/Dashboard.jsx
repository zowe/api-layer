import { Text, Button, ButtonGroup, Dropdown } from 'mineral-ui';
import { IconArrowDropDown } from 'mineral-ui-icons';
import React, { Component } from 'react';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import './Dashboard.css';
import Tile from '../Tile/Tile';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ErrorDialog from '../Error/ErrorDialog';
import WizardContainer from '../Wizard/WizardContainer';
import { enablerData } from '../Wizard/wizard-config';

export default class Dashboard extends Component {
    constructor(props) {
        super(props);
        this.state = {
            WIP: false,
        };
    }

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

    toggleWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
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
                <ButtonGroup id="onboarding-btns">
                    {this.state.WIP ? (
                        <span />
                    ) : (
                        <Dropdown iconEnd={<IconArrowDropDown />} data={enablerData}>
                            <Button id="wizard-YAML-button" iconEnd={<IconArrowDropDown />} onClick={this.toggleWizard}>
                                Onboard New API
                            </Button>
                        </Dropdown>
                    )}
                    <Button id="refresh-api-button" onClick={this.refreshStaticApis}>
                        Refresh Static APIs
                    </Button>
                </ButtonGroup>
                <WizardContainer />
                <Spinner isLoading={isLoading} />
                {fetchTilesError && (
                    <div className="no-tiles-container">
                        <br />
                        <br />
                        <Text element="h3">Tile details could not be retrieved, the following error was returned:</Text>
                        {error}
                    </div>
                )}
                <ErrorDialog refreshedStaticApisError={refreshedStaticApisError} clearError={clearError} />
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
