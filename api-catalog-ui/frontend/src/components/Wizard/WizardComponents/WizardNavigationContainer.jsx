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
import WizardNavigation from './WizardNavigation';
import { changeWizardCategory, nextWizardCategory, validateInput } from '../../../actions/wizard-actions';
import { assertAuthorization } from '../../../actions/wizard-fetch-actions';

const mapStateToProps = state => ({
    selectedCategory: state.wizardReducer.selectedCategory,
    inputData: state.wizardReducer.inputData,
    navsObj: state.wizardReducer.navsObj,
});

const mapDispatchToProps = {
    nextWizardCategory,
    changeWizardCategory,
    validateInput,
    assertAuthorization,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(WizardNavigation);
