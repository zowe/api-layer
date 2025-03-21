/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {
    FETCH_TILES_FAILED,
    FETCH_TILES_REQUEST,
    FETCH_TILES_RETRY,
    FETCH_TILES_STOP,
    FETCH_TILES_SUCCESS,
    FETCH_NEW_SERVICE_REQUEST,
    FETCH_NEW_SERVICE_SUCCESS,
    FETCH_SERVICE_STOP,
    FETCH_SERVICE_FAILED
} from '../constants/catalog-tile-constants';

const tilesReducerDefaultState = {
    tile: {},
    tiles: [],
    services: [],
    service: {},
    id: '',
    tilesLoading: false,
    error: null,
};

const tilesReducer = (state = tilesReducerDefaultState, action = {}) => {
    switch (action.type) {
        case FETCH_TILES_SUCCESS:
            return {
                ...state,
                services: [...state.services],
                tiles: [...action.payload],
                tilesLoading: false,
                error: null,
            };
        case FETCH_NEW_SERVICE_SUCCESS:
            return {
                ...state,
                service: action.payload,
                error: null,
                serviceLoading: false,
            };
        case FETCH_TILES_FAILED:
            return {
                tiles: state.tiles,
                services: state.services,
                id: '',
                error: action.payload,
            };
        case FETCH_SERVICE_FAILED:
            return {
                id: '',
                error: action.payload,
            };
        case FETCH_TILES_REQUEST:
            return {
                tiles: [],
                services: state.services,
                id: action.payload,
                tilesLoading: true,
                error: null,
            };
       case FETCH_NEW_SERVICE_REQUEST:
            return {
                ...state,
                id: action.payload,
                serviceLoading: true,
                error: null,
            };
        case FETCH_TILES_RETRY:
            return state;
        case FETCH_TILES_STOP:
            return state;
        case FETCH_SERVICE_STOP:
            return state;
        default:
            return state;
    }
};

export default tilesReducer;
