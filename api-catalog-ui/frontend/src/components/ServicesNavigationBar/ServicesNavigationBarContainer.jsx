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
import ServicesNavigationBar from './ServicesNavigationBar';
import { getFilteredServices } from '../../selectors/selectors';
import { clear, filterText } from '../../actions/filter-actions';
import { storeCurrentTileId } from '../../actions/catalog-tile-actions';

const mapStateToProps = (state) => ({
    searchCriteria: state.filtersReducer.text,
    services: getFilteredServices(state.tilesReducer.services, state.filtersReducer.text),
});
const mapDispatchToProps = (dispatch) => ({
    filterText: (text) => dispatch(filterText(text)),
    clear: () => dispatch(clear()),
    storeCurrentTileId: (id) => dispatch(storeCurrentTileId(id)),
});

export default connect(mapStateToProps, mapDispatchToProps)(ServicesNavigationBar);
