/* eslint-disable no-undef */

import { sendError, clearAllErrors } from './error-actions';
import {ApiError, CLEAR_ALL_ERRORS, MessageType, SEND_ERROR} from "../constants/error-constants";


describe('>>> Error actions tests', () => {
    it('should create an action to clear all errors', () => {
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
});
