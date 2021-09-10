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
import { deleteCategoryConfig, updateWizardData, validateInput } from '../../../actions/wizard-actions';
import WizardInputs from './WizardInputs';

const mapStateToProps = state => ({
    inputData: state.wizardReducer.inputData,
    tiles: state.tilesReducer.tiles,
});

const mapDispatchToProps = {
    updateWizardData,
    deleteCategoryConfig,
    validateInput,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(WizardInputs);
