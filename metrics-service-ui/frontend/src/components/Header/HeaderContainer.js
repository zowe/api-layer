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
import { userActions } from '../../actions/user-actions';
import Header from './Header';

const mapStateToProps = () => ({});

const mapDispatchToProps = {
    logout: () => userActions.logout(),
};

export default connect(mapStateToProps, mapDispatchToProps)(Header);
