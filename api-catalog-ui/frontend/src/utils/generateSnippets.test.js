/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { generateSnippet } from './generateSnippets';

describe('>>> Code snippet generator', () => {
    test('generate a snippet', () => {
        const system = {
            Im: {
                fromJS: jest.fn().mockImplementation(() => ({
                    title: 'foo',
                    syntax: 'js',
                    fn: jest.fn().mockImplementation(() => 'bar'),
                })),
            },
        };
        const title = 'foo';
        const syntax = 'js';
        const target = {};
        const result = generateSnippet(system, title, syntax, target);
        expect(system.Im.fromJS).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            title: 'foo',
            syntax: 'js',
            fn: expect.any(Function),
        });
    });

    test('generateSnippet function should return correct snippet content', () => {
        const system = {
            Im: {
                fromJS: (obj) => obj,
            },
        };
        const title = 'Java Unirest';
        const syntax = 'java';
        const target = 'java_unirest';

        const spec = {
            paths: {
                '/path/to/api': {
                    get: {
                        responses: {
                            200: {
                                description: 'Response description',
                                schema: {
                                    $ref: '#/definitions/SomeSchema',
                                },
                            },
                        },
                    },
                },
            },
        };

        const oasPathMethod = {
            path: '/path/to/api',
            method: 'get',
        };

        const req = {
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };

        const result = generateSnippet(system, title, syntax, target).fn(req);
        const expectedResult =
            // eslint-disable-next-line no-useless-concat
            'HttpResponse<String> response = Unirest.get("http://undefinedundefined/path/to/api")\n' + '  .asString();';

        expect(result).toEqual(expectedResult);
    });
});
