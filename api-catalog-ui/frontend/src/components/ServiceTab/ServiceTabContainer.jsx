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
import { withRouter } from 'react-router-dom';
import { fetchTilesStop } from '../../actions/catalog-tile-actions';
import { selectService } from '../../actions/selected-service-actions';
import ServiceTab from './ServiceTab';

const mapStateToProps = (state) => ({
    tiles: state.tilesReducer.tiles,
    originalTiles: state.tilesReducer.originalTiles,
    selectedService: state.selectedServiceReducer.selectedService,
    selectedTile: state.selectedServiceReducer.selectedTile,
    currentTileId: state.tilesReducer.currentTileId,
});

const mapDispatchToProps = (dispatch) => ({
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    selectService: (service, tileId) => dispatch(selectService(service, tileId)),
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ServiceTab));
