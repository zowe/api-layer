/* eslint-disable no-undef */

import {
    CLEAR_ERROR,
    REFRESH_STATIC_APIS_ERROR,
    REFRESH_STATIC_APIS_SUCCESS
} from "../constants/refresh-static-apis-constants";
import {refreshStaticApisSuccess, refreshStaticApisError, clearError, refreshedStaticApi} from "./refresh-static-apis-actions";
import {ApiError, MessageType} from "../constants/error-constants";

describe('>>> Refresh static apis actions tests', () => {
    it('should create action to refresh static apis', function () {
        const expectedAction = {
            type: REFRESH_STATIC_APIS_SUCCESS,
            refreshTimestamp: 1234,
        };
        const response = refreshStaticApisSuccess();
        response.refreshTimestamp = 1234;
        expect(response).toEqual(expectedAction);
    });

    it('should create action if there is an error when refreshing', function () {
        const error = new ApiError('ABC123', 123, new MessageType(40, 'ERROR', 'E'), 'Bad stuff happened');
        const expectedAction = {
            type: REFRESH_STATIC_APIS_ERROR,
            error: error,
        };
        const response = refreshStaticApisError(error);
        expect(response).toEqual(expectedAction);
    });

    it('should create action to clear error', function () {
        const expectedAction = {
            type: CLEAR_ERROR,
            error: null,
        };
        const response = clearError();
        expect(response).toEqual(expectedAction);
    });

});
