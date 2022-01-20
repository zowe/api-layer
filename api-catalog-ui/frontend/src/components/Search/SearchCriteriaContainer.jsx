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
import { filterText, clear } from '../../actions/filter-actions';
import SearchCriteria from './SearchCriteria';

const mapStateToProps = (state) => ({
    filterText: state.filtersReducer.text,
    criteria: state.filtersReducer.text,
});

const mapDispatchToProps = (dispatch) => ({
    filterText: (text) => dispatch(filterText(text)),
    clear: () => dispatch(clear()),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchCriteria);
