import { combineReducers } from 'redux';
import filtersReducer from './filter-reducer';
import loadingReducer from './loading-reducer.jx';
import authenticationReducer from './authentication-reducer';

const reducers = {
    filtersReducer,
    loadingReducer,
    authenticationReducer,
};

// eslint-disable-next-line import/prefer-default-export
export const rootReducer = combineReducers(reducers);
