import {connect} from 'react-redux';
import Dashboard from './Dashboard';
import {
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesStop,
    fetchTilesSuccess,
} from '../../actions/catalog-tile-actions';
import {clearService} from '../../actions/selected-service-actions';
import {clear, filterText} from '../../actions/filter-actions';
import {createLoadingSelector, getVisibleTiles} from '../../selectors/selectors';

const loadingSelector = createLoadingSelector(['FETCH_TILES']);

const mapStateToProps = state => ({
    searchCriteria: state.filtersReducer.text,
    tiles: getVisibleTiles(state.tilesReducer.tiles, state.filtersReducer.text),
    fetchTilesError: state.tilesReducer.error,
    isLoading: loadingSelector(state),
});

const mapDispatchToProps = {
    clearService,
    fetchTilesStart,
    fetchTilesSuccess,
    fetchTilesFailed,
    fetchTilesStop,
    filterText,
    clear,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Dashboard);
