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

const mapStateToProps = (state) => ({
    searchCriteria: state.filtersReducer.text,
    originalTiles: getFilteredServices(state.tilesReducer.originalTiles, state.filtersReducer.text),
});
const mapDispatchToProps = {
    filterText,
    clear,
};

export default connect(mapStateToProps, mapDispatchToProps)(ServicesNavigationBar);
