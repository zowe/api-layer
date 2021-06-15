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
import { withRouter } from 'react-router-dom';
import Login from './Login';
import { userActions } from '../../actions/user-actions';
import { createLoadingSelector } from '../../selectors';

const loadingSelector = createLoadingSelector(['USERS_LOGIN']);

const mapStateToProps = (state) => ({
    authentication: state.authenticationReducer,
    isFetching: loadingSelector(state),
});

const mapDispatchToProps = {
    login: (credentials) => userActions.login(credentials),
    logout: () => userActions.logout(),
};

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Login));
