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
import GraphQLUIApiml from './GraphQLUIApiml';

const mapStateToProps = (state) => ({
    selectedService: state.selectedServiceReducer.selectedService,
    tiles: state.tilesReducer.tiles,
});

const mapDispatchToProps = {};

export default connect(mapStateToProps, mapDispatchToProps)(GraphQLUIApiml);
