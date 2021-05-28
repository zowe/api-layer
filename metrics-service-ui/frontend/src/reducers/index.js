/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { combineReducers } from 'redux';
import filtersReducer from './filter-reducer';
import loadingReducer from './loading-reducer';
import authenticationReducer from './authentication-reducer';
import errorReducer from './error-reducer';

const reducers = {
    filtersReducer,
    loadingReducer,
    authenticationReducer,
    errorReducer,
};

// eslint-disable-next-line import/prefer-default-export
export const rootReducer = combineReducers(reducers);
