/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { HashRouter } from 'react-router-dom';
import { PersistGate } from 'redux-persist/integration/react';
import { applyMiddleware, compose, createStore } from 'redux';
import { createEpicMiddleware } from 'redux-observable';
// eslint-disable-next-line import/no-extraneous-dependencies
import { ajax } from 'rxjs/ajax';
import logger from 'redux-logger';
import log from 'loglevel';
import { createBlacklistFilter } from 'redux-persist-transform-filter';
import reduxCatch from 'redux-catch';
import storageSession from 'redux-persist/lib/storage/session';
import { persistReducer, persistStore } from 'redux-persist';
import thunk from 'redux-thunk';

import Spinner from './components/Spinner/Spinner';
import { AsyncAppContainer } from './components/App/AsyncModules';
import { rootReducer } from './reducers';
import { sendError } from './actions/error-actions';

function errorHandler(error, getState, lastAction, dispatch) {
    log.error(error);
    log.debug('current state', getState());
    log.debug('last action was', lastAction);
    dispatch(sendError(`Action: ${lastAction.type} => ${error.message}`));
}

// do not save authentication errors
const authenticationReducerBlacklistFilter = createBlacklistFilter('authenticationReducer', ['error']);

const persistConfig = {
    key: 'root',
    storage: storageSession,
    blacklist: ['filtersReducer', 'loadingReducer'],
    transforms: [authenticationReducerBlacklistFilter],
};

const epicMiddleware = createEpicMiddleware({
    dependencies: { ajax },
});
const composeEnhancers = compose;
const middlewares = [epicMiddleware, thunk, reduxCatch(errorHandler)];

if (process.env.NODE_ENV !== 'production') {
    middlewares.push(logger);
}

const persistedReducer = persistReducer(persistConfig, rootReducer);
const store = createStore(persistedReducer, composeEnhancers(applyMiddleware(...middlewares)));
const persistor = persistStore(store);

ReactDOM.render(
    <HashRouter>
        <Provider store={store}>
            <PersistGate loading={<Spinner isLoading />} persistor={persistor}>
                <AsyncAppContainer />
            </PersistGate>
        </Provider>
    </HashRouter>,
    document.getElementById('root')
);
