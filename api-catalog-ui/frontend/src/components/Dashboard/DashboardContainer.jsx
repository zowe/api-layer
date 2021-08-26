import { connect } from 'react-redux';
import Dashboard from './Dashboard';
import {
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesSuccess,
    fetchTilesStop,
} from '../../actions/catalog-tile-actions';
import { clearService } from '../../actions/selected-service-actions';
import { filterText, clear } from '../../actions/filter-actions';
import { createLoadingSelector, getVisibleTiles } from '../../selectors/selectors';
import { clearError, refreshedStaticApi } from '../../actions/refresh-static-apis-actions';
import { selectEnabler, wizardToggleDisplay } from '../../actions/wizard-actions';
import { assertAuthorization } from '../../actions/wizard-fetch-actions';

const loadingSelector = createLoadingSelector(['FETCH_TILES']);

const mapStateToProps = state => ({
    searchCriteria: state.filtersReducer.text,
    tiles: getVisibleTiles(state.tilesReducer.tiles, state.filtersReducer.text),
    fetchTilesError: state.tilesReducer.error,
    isLoading: loadingSelector(state),
    refreshedStaticApisError: state.refreshStaticApisReducer.error,
    refreshTimestamp: state.refreshStaticApisReducer.refreshTimestamp,
    wizardIsVisible: state.wizardReducer.wizardIsVisible,
});

const mapDispatchToProps = {
    clearService,
    fetchTilesStart,
    fetchTilesSuccess,
    fetchTilesFailed,
    fetchTilesStop,
    filterText,
    clear,
    refreshedStaticApi,
    clearError,
    assertAuthorization,
    wizardToggleDisplay,
    selectEnabler,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Dashboard);
