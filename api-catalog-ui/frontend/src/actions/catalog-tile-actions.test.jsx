/* eslint-disable no-undef */

import * as constants from "../constants/catalog-tile-constants";
import * as actions from "./catalog-tile-actions";


describe('>>> Catalog tiles actions tests', () => {
    it('should create when fetching tiles has failed', () => {
        const expectedAction = {
            type: constants.FETCH_TILES_FAILED,
        };

        expect(actions.fetchTilesFailed()).toEqual(expectedAction);
    });

    it('should create when fetching tiles is successful', () => {
        const expectedAction = {
            type: constants.FETCH_TILES_SUCCESS,
            payload: [],
        };

        expect(actions.fetchTilesSuccess([])).toEqual(expectedAction);
    });

    it('should create when start fetching one tile', () => {
        const expectedAction = {
            type: constants.FETCH_TILES_REQUEST,
            payload: '',
        };

        expect(actions.fetchTilesStart('')).toEqual(expectedAction);
    });

    it('should create when stop fetching tiles', () => {
        const expectedAction = {
            type: constants.FETCH_TILES_STOP,
        };

        expect(actions.fetchTilesStop()).toEqual(expectedAction);
    });

    it('should create when retry fetching of tiles', () => {
        const expectedAction = {
            type: constants.FETCH_TILES_RETRY,
        };

        expect(actions.fetchTilesRetry()).toEqual(expectedAction);
    });
});
