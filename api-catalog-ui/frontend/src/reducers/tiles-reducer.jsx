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
    STORE_CURRENT_TILEID,
    STORE_ORIGINAL_TILES,
} from '../constants/catalog-tile-constants';

const tilesReducerDefaultState = {
    tile: {},
    tiles: [],
    originalTiles: [],
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
                originalTiles: state.originalTiles,
                tiles: [...action.payload],
                error: null,
            };
        case STORE_ORIGINAL_TILES:
            return { ...state, originalTiles: action.payload, error: null };
        case STORE_CURRENT_TILEID:
            return { ...state, currentTileId: action.payload, error: null };
        case FETCH_TILES_FAILED:
            return {
                tiles: state.tiles,
                currentTileId: state.currentTileId,
                originalTiles: state.originalTiles,
                id: '',
                error: action.payload,
            };
        case FETCH_TILES_REQUEST:
            return {
                tiles: [],
                currentTileId: state.currentTileId,
                originalTiles: state.originalTiles,
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
