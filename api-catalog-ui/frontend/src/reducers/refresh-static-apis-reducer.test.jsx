/* eslint-disable no-undef */
import refreshStaticApisReducer from './refresh-static-apis-reducer';
import {
    REFRESH_STATIC_APIS_SUCCESS,
    REFRESH_STATIC_APIS_ERROR,
    CLEAR_ERROR
} from "../constants/refresh-static-apis-constants";

describe('>>> Refresh static apis reducer tests', () => {
    it('should handle REFRESH_STATIC_APIS_SUCCESS', function () {
        const expectedState = {
            refreshTimestamp: 1234,
            error: null,
        };
        expect(
            refreshStaticApisReducer(
                {
                    refreshTimestamp: 1234,
                },
                {
                    type: REFRESH_STATIC_APIS_SUCCESS,
                    refreshTimestamp: 1234,
                }
            )
        ).toEqual(expectedState);
    });

    it('should handle REFRESH_STATIC_APIS_ERROR', function () {
        const expectedState = {
            error: 'test',
        };
        expect(
            refreshStaticApisReducer(
                {
                },
                {
                    type: REFRESH_STATIC_APIS_ERROR,
                    error: 'test',
                }
            )
        ).toEqual(expectedState);
    });

    it('should handle CLEAR_ERROR', function () {
        const expectedState = {
            error: null,
        };
        expect(
            refreshStaticApisReducer(
                {
                },
                {
                    type: CLEAR_ERROR,
                    error: null,
                }
            )
        ).toEqual(expectedState);
    });

    it('should handle DEFAULT', () => {
        const expectedState = {
            refreshTimestamp: 1234,
        };

        expect(refreshStaticApisReducer({ refreshTimestamp: 1234 }, { type: 'UNKNOWN', refreshTimestamp: 1234 })).toEqual(expectedState);
    });

});
