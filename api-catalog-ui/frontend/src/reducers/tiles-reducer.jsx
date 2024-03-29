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
    FETCH_NEW_TILES_REQUEST,
    FETCH_TILES_RETRY,
    FETCH_TILES_STOP,
    FETCH_TILES_SUCCESS,
    FETCH_NEW_TILES_SUCCESS,
    STORE_CURRENT_TILEID,
} from '../constants/catalog-tile-constants';

const tilesReducerDefaultState = {
    tile: {},
    tiles: [],
    services: [],
    id: '',
    currentTileId: '',
    error: null,
};

const tilesReducer = (state = tilesReducerDefaultState, action = {}) => {
    switch (action.type) {
        case FETCH_TILES_SUCCESS:
            return {
                ...state,
                currentTileId: state.currentTileId,
                services: [...state.services],
                tiles: [...action.payload],
                error: null,
            };
        case FETCH_NEW_TILES_SUCCESS:
            return {
                ...state,
                currentTileId: state.currentTileId,
                tiles: [...state.tiles],
                services: [...action.payload],
                error: null,
            };
        case STORE_CURRENT_TILEID:
            return { ...state, currentTileId: action.payload, error: null };
        case FETCH_TILES_FAILED:
            return {
                tiles: state.tiles,
                services: state.services,
                currentTileId: state.currentTileId,
                id: '',
                error: action.payload,
            };
        case FETCH_TILES_REQUEST:
            return {
                tiles: [],
                services: state.services,
                currentTileId: state.currentTileId,
                id: action.payload,
                error: null,
            };
        case FETCH_NEW_TILES_REQUEST:
            return {
                services: [],
                tiles: state.tiles,
                currentTileId: state.currentTileId,
                id: action.payload,
                error: null,
            };
        case FETCH_TILES_RETRY:
            return state;
        case FETCH_TILES_STOP:
            return state;
        default:
            return state;
    }
};

export default tilesReducer;
