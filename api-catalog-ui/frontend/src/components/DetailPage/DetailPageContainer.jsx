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
    fetchTilesSuccess, fetchNewService, fetchServiceStop, fetchServiceFailed,
} from '../../actions/catalog-tile-actions';
import {clearService, selectService} from '../../actions/selected-service-actions';
import { createLoadingSelector } from '../../selectors/selectors';
import DetailPage from './DetailPage';

const loadingSelector = createLoadingSelector(['FETCH']);

const mapStateToProps = (state) => ({
    tile: state.tilesReducer.tile,
    services: state.tilesReducer.services,
    tiles: state.tilesReducer.tiles,
    fetchTilesError: state.tilesReducer.error,
    fetchServiceError: state.tilesReducer.error,
    selectedTile: state.selectedServiceReducer.selectedTile,
    selectedService: state.selectedServiceReducer.selectedService,
    isLoading: loadingSelector(state),
    authentication: state.authenticationReducer,
    service: state.tilesReducer.service,
    serviceLoading: state.tilesReducer.serviceLoading,
    tilesLoading: state.tilesReducer.tilesLoading,
});

const mapDispatchToProps = (dispatch) => ({
    fetchTilesStart: (id) => dispatch(fetchTilesStart(id)),
    fetchTilesSuccess: (tiles) => dispatch(fetchTilesSuccess(tiles)),
    fetchTilesFailed: (error) => dispatch(fetchTilesFailed(error)),
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    clearService: () => dispatch(clearService()),
    selectService: (service, tileId) => dispatch(selectService(service, tileId)),
    fetchServiceStop: () => dispatch(fetchServiceStop()),
    fetchNewService: (id) => dispatch(fetchNewService(id)),
    fetchServiceFailed: (id) => dispatch(fetchServiceFailed(id)),

});

export default connect(mapStateToProps, mapDispatchToProps)(DetailPage);
