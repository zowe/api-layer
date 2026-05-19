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
import { useNavigate } from 'react-router';
import Login from './Login';
import { userActions } from '../../actions/user-actions';
import { createLoadingSelector } from '../../selectors';

const loadingSelector = createLoadingSelector(['USERS_LOGIN']);

const mapStateToProps = (state) => ({
    authentication: state.authenticationReducer,
    isFetching: loadingSelector(state),
});

function LoginContainer(props) {
    const navigate = useNavigate();
    const { dispatch, authentication, isFetching } = props;

    const boundActions = {
        login: (credentials) => dispatch(userActions.login(credentials, navigate)),
        logout: () => dispatch(userActions.logout(navigate)),
    };

    return <Login {...props} {...boundActions} authentication={authentication} isFetching={isFetching} />;
}

export default connect(mapStateToProps)(LoginContainer);
