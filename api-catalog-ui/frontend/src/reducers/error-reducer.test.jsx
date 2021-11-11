/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */
import { SEND_ERROR, CLEAR_ALL_ERRORS, ApiError, MessageType, GATEWAY_UP } from '../constants/error-constants';
import errorReducer from './error-reducer';

describe('>>> Error reducer tests', () => {
    it('should return default state in the default action', () => {
        expect(errorReducer({ errors: ['bar', 'foo'] }, {})).toEqual({ errors: ['bar', 'foo'] });
    });

    it('should handle CLEAR_ALL_ERRORS', () => {
        const expectedState = {
            errors: [],
        };

        expect(errorReducer({ errors: ['bar', 'foo'] }, { type: CLEAR_ALL_ERRORS, payload: null })).toEqual(
            expectedState
        );
    });

    it('should handle SEND_ERROR', () => {
        const error = new ApiError('ABC123', 123, new MessageType(40, 'ERROR', 'E'), 'Bad stuff happened');
        const expectedState = {
            errors: [error],
        };

        expect(errorReducer({ errors: [] }, { type: SEND_ERROR, payload: error })).toEqual(expectedState);
    });
});
