/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import _ from 'lodash';

// check the current action against any states which are requests
// eslint-disable-next-line import/prefer-default-export
export const createLoadingSelector = (actions) => (state) =>
    _(actions).some((action) => _.get(state.loadingReducer, action));
