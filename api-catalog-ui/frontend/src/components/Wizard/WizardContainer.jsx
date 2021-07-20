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
import WizardDialog from './WizardDialog';
import { wizardToggleDisplay, changedEnablers } from '../../actions/wizard-actions';
import { refreshedStaticApi } from '../../actions/refresh-static-apis-actions';

const mapStateToProps = state => ({
    wizardIsOpen: state.wizardReducer.wizardIsOpen,
    enablerName: state.wizardReducer.enablerName,
    inputData: state.wizardReducer.inputData,
    enablerChanged: state.wizardReducer.enablerChanged,
});

const mapDispatchToProps = {
    wizardToggleDisplay,
    refreshedStaticApi,
    changedEnablers,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(WizardDialog);
