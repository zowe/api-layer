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
import { fetchTilesStop } from '../../actions/catalog-tile-actions';
import { selectService } from '../../actions/selected-service-actions';
import ServiceTab from './ServiceTab';
import {createLoadingSelector} from "../../selectors/selectors";

const loadingSelector = createLoadingSelector(['FETCH_TILES']);
const mapStateToProps = (state) => ({
    selectedService: state.selectedServiceReducer.selectedService,
    selectedTile: state.selectedServiceReducer.selectedTile,
    currentTileId: state.tilesReducer.currentTileId,
    services: state.tilesReducer.services,
    isLoading: loadingSelector(state),
    service: state.tilesReducer.service,
});

const mapDispatchToProps = (dispatch) => ({
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    selectService: (service, tileId) => dispatch(selectService(service, tileId)),
});


const withRouter = (ServiceTab) =>{
    return (props) =>{
        return <ServiceTab {...props}/>
    }
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ServiceTab));
