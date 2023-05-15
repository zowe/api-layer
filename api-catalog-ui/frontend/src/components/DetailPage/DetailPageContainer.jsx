/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { connect } from 'react-redux';
import {
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesStop,
    fetchTilesSuccess,
} from '../../actions/catalog-tile-actions';
import { clearService } from '../../actions/selected-service-actions';
import { createLoadingSelector } from '../../selectors/selectors';
import DetailPage from './DetailPage';

const loadingSelector = createLoadingSelector(['FETCH_TILES']);

const mapStateToProps = (state) => ({
    tile: state.tilesReducer.tile,
    tiles: state.tilesReducer.tiles,
    fetchTilesError: state.tilesReducer.error,
    originalTiles: state.tilesReducer.originalTiles,
    selectedTile: state.selectedServiceReducer.selectedTile,
    selectedServiceId: state.selectedServiceReducer.selectedService.serviceId,
    isLoading: loadingSelector(state),
});

const mapDispatchToProps = (dispatch) => ({
    fetchTilesStart: (id) => dispatch(fetchTilesStart(id)),
    fetchTilesSuccess: (tiles) => dispatch(fetchTilesSuccess(tiles)),
    fetchTilesFailed: (error) => dispatch(fetchTilesFailed(error)),
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    clearService: () => dispatch(clearService()),
});

export default connect(mapStateToProps, mapDispatchToProps)(DetailPage);
