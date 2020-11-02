import { connect } from 'react-redux';
import {
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesStop,
    fetchTilesSuccess,
    fetchServiceDoc,
} from '../../actions/catalog-tile-actions';
import { clearService } from '../../actions/selected-service-actions';
import { createLoadingSelector } from '../../selectors/selectors';
import DetailPage from './DetailPage';

const loadingSelector = createLoadingSelector(['FETCH_TILES']);

const mapStateToProps = state => ({
    tile: state.tilesReducer.tile,
    tiles: state.tilesReducer.tiles,
    fetchTilesError: state.tilesReducer.error,
    selectedTile: state.selectedServiceReducer.selectedTile,
    selectedServiceId: state.selectedServiceReducer.selectedService.serviceId,
    isLoading: loadingSelector(state),
});

const mapDispatchToProps = dispatch => ({
    fetchTilesStart: id => dispatch(fetchTilesStart(id)),
    fetchTilesSuccess: tiles => dispatch(fetchTilesSuccess(tiles)),
    fetchTilesFailed: error => dispatch(fetchTilesFailed(error)),
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    fetchServiceDoc: (id, version) => dispatch(fetchServiceDoc(id, version)),
    clearService: () => dispatch(clearService()),
});

export default connect(
    mapStateToProps,
    mapDispatchToProps,
)(DetailPage);
