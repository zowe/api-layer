/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import formatError from './ErrorFormatter';

describe('>>> ErrorFormatter tests', () => {
    it('should return default message when error is null', () => {
        const error = formatError(null);
        const expectedError = 'Could not determine error';
        expect(error.props.children).toContain(expectedError);
    });

    it('should return default message when error is undefined', () => {
        const error = formatError(undefined);
        const expectedError = 'Could not determine error';
        expect(error.props.children).toContain(expectedError);
    });

    it('should return default message when error does not contain key', () => {
        const error = formatError('Some error');
        const expectedError = 'Could not determine error';
        expect(error.props.children).toContain(expectedError);
    });

    it('should format error message', () => {
        const error = {
            id: 'id',
            timestamp: 'timestamp',
            key: 'org.zowe.apiml.security.invalidUsername',
            text: 'an error',
            error: {
                response: {
                    messages: [
                        {
                            messageType: {
                                levelStr: 'ERROR',
                            },
                            messageNumber: 'ZWEAS120E',
                            messageContent: 'Invalid Credentials',
                            messageKey: 'org.zowe.apiml.security.invalidUsername',
                        },
                    ],
                },
            },
        };
        const formattedError = formatError(error);
        expect(formattedError.props.children[1]).toContain('ZWEAS120E');
        expect(formattedError.props.children[4]).toContain('Invalid Credentials');
    });

    it('should format warning message', () => {
        const error = {
            id: 'id',
            timestamp: 'timestamp',
            key: 'org.zowe.apiml.security.invalidUsername',
            text: 'an error',
            error: {
                response: {
                    messages: [
                        {
                            messageType: {
                                levelStr: 'WARNING',
                            },
                            messageNumber: 'ZWEAS120E',
                            messageContent: 'Invalid Credentials',
                            messageKey: 'org.zowe.apiml.security.invalidUsername',
                        },
                    ],
                },
            },
        };
        const formattedError = formatError(error);
        expect(formattedError.props.children[1]).toContain('ZWEAS120E');
        expect(formattedError.props.children[4]).toContain('Invalid Credentials');
    });

    it('should format generic message', () => {
        const error = {
            id: 'id',
            timestamp: 'timestamp',
            key: 'org.zowe.apiml.security.invalidUsername',
            text: 'an error',
            error: {
                response: {
                    messages: [
                        {
                            messageType: {
                                levelStr: 'INFO',
                            },
                            messageNumber: 'ZWEAS120E',
                            messageContent: 'Invalid Credentials',
                            messageKey: 'org.zowe.apiml.security.invalidUsername',
                        },
                    ],
                },
            },
        };
        const formattedError = formatError(error);
        expect(formattedError.props.children[1]).toContain('ZWEAS120E');
        expect(formattedError.props.children[4]).toContain('Invalid Credentials');
    });
});
