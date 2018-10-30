import { connect } from 'react-redux';
import {
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesSuccess,
    fetchTilesStop,
} from '../../actions/catalog-tile-actions';
import DetailPage from './DetailPage';
import { createLoadingSelector } from '../../selectors/selectors';

const loadingSelector = createLoadingSelector(['FETCH_TILES']);

const mapStateToProps = state => ({
    tile: state.tilesReducer.tile,
    tiles: state.tilesReducer.tiles,
    fetchTilesError: state.tilesReducer.error,
    isLoading: loadingSelector(state),
});

const mapDispatchToProps = dispatch => ({
    fetchTilesStart: id => dispatch(fetchTilesStart(id)),
    fetchTilesSuccess: tiles => dispatch(fetchTilesSuccess(tiles)),
    fetchTilesFailed: error => dispatch(fetchTilesFailed(error)),
    fetchTilesStop: () => dispatch(fetchTilesStop()),
});

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(DetailPage);
