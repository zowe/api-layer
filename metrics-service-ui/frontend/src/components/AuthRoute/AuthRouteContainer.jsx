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

import AuthRoute from './AuthRoute';

const mapStateToProps = (state) => {
    // TODO will we use this state? Need to set redux state in AuthRoute component instead of using component state
    const authenticated = !!state.authenticationReducer.sessionOn;
    return { authenticated };
};

export default connect(mapStateToProps)(AuthRoute);
