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
import { createYamlObject } from '../../../actions/wizard-actions';
import YAMLVisualizer from './YAMLVisualizer';

const mapStateToProps = state => ({
    inputData: state.wizardReducer.inputData,
    yamlObject: state.wizardReducer.yamlObject,
});

const mapDispatchToProps = {
    createYamlObject,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(YAMLVisualizer);
