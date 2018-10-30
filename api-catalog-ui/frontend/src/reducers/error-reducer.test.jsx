/* eslint-disable no-undef */
import { sendError, clearAllErrors } from '../actions/error-actions';
import { SEND_ERROR, CLEAR_ALL_ERRORS, ApiError, MessageType } from '../constants/error-constants';
import errorReducer from './error-reducer';

describe('>>> Error reducer tests', () => {
    it('should create an action to clear errors', () => {
        const expectedAction = {
            type: CLEAR_ALL_ERRORS,
            payload: null,
        };

        expect(clearAllErrors()).toEqual(expectedAction);
    });

    it('should create an action to send a message', () => {
        const error = new ApiError('ABC123', 123, new MessageType(40, 'ERROR', 'E'), 'Bad stuff happened');
        const err = { id: '123', timestamp: '456', error };

        const expectedAction = {
            type: SEND_ERROR,
            payload: err,
        };
        const responseError = sendError(error);
        responseError.payload.id = '123';
        responseError.payload.timestamp = '456';
        expect(responseError).toEqual(expectedAction);
    });

    it('should handle CLEAR_ALL_ERRORS', () => {
        const expectedState = {
            errors: [],
        };

        expect(errorReducer({ errors: [] }, { type: CLEAR_ALL_ERRORS, payload: null })).toEqual(expectedState);
    });

    it('should handle SEND_ERROR', () => {
        const error = new ApiError('ABC123', 123, new MessageType(40, 'ERROR', 'E'), 'Bad stuff happened');
        const expectedState = {
            errors: [error],
        };

        expect(errorReducer({ errors: [] }, { type: SEND_ERROR, payload: error })).toEqual(expectedState);
    });
});
