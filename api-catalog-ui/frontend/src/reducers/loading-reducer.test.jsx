/* eslint-disable no-undef */

import loadingReducer from './loading-reducer';

describe('>>> Loading reducer tests', () => {
    it('should return current state if not a *_REQUEST', () => {
        const state = { payload: 'payload' };
        const action = { type: 'FETCH_TILES_SUCCESS' };
        const expectedState = { FETCH_TILES: false, payload: 'payload' };
        expect(loadingReducer(state, action)).toEqual(expectedState);
    });

    it('should return isRequest if state if state is a *_REQUEST', () => {
        const state = { payload: 'payload' };
        const expectedState = { FETCH_TILES: true, payload: 'payload' };
        const action = { type: 'FETCH_TILES_REQUEST' };

        expect(loadingReducer(state, action)).toEqual(expectedState);
    });
});
