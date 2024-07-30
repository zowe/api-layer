/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as OpenAPISnippet from 'openapi-snippet';
import { generateSnippet, getSnippetContent, wrapSelectors } from './generateSnippets';

describe('>>> Code snippet generator', () => {
    it('generate a snippet', () => {
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

    it('generateSnippet function should return correct snippet content', () => {
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
            get: jest.fn(),
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

    it('generate a snippet for endpoint with query parameter', () => {
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
                        parameters: [
                            {
                                name: 'parameter',
                                in: 'query',
                                required: true,
                                type: 'string',
                            },
                        ],
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

        const query = { parameter: 'value' };

        const req = {
            get: jest.fn((key) => {
                if (key === 'query') {
                    return query;
                }
                return undefined;
            }),
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };

        const codeSnippet = {
            endpoint: '/path/to/api?parameter=value',
            language: 'java',
            codeBlock: 'Some java code;',
        };

        const result = generateSnippet(system, title, syntax, target, codeSnippet).fn(req);
        const expectedResult = 'Some java code;';

        expect(result).toEqual(expectedResult);
    });

    it('should call mutatedRequestFor with path and method', () => {
        const ori = jest.fn();
        const requestFor = wrapSelectors.spec.wrapSelectors.requestFor(ori);
        const state = {
            json: {},
        };
        const path = '/some/path';
        const method = 'GET';
        requestFor(state, path, method);
        expect(ori).toHaveBeenCalledWith(path, method);
        expect(ori).toHaveBeenCalledTimes(1);
    });

    it('should wrap the mutatedRequestFor selector', () => {
        const ori = jest.fn();
        const mutatedRequestFor = wrapSelectors.spec.wrapSelectors.mutatedRequestFor(ori);
        const state = {
            json: {},
        };
        const path = '/some/path';
        const method = 'GET';
        mutatedRequestFor(state, path, method);
        expect(ori).toHaveBeenCalledWith(path, method);
        expect(ori).toHaveBeenCalledTimes(1);
    });

    it('should return a snippet for the given system, title, syntax, and target', () => {
        const system = {
            Im: {
                fromJS: jest.fn().mockImplementation(() => ({
                    title: 'test title',
                    syntax: 'test syntax',
                    fn: (req) => {
                        // get extended info about request
                        const { spec, oasPathMethod } = req.toJS();
                        const { path, method } = oasPathMethod;
                        // run OpenAPISnippet for target node
                        // eslint-disable-next-line no-use-before-define
                        const targets = ['test target'];
                        let snippet;
                        try {
                            // set request snippet content
                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets).snippets[0]
                                .content;
                        } catch (err) {
                            snippet = JSON.stringify(snippet);
                        }
                        return snippet;
                    },
                })),
            },
        };

        const title = 'test title';
        const syntax = 'test syntax';
        const target = 'test target';

        const result = generateSnippet(system, title, syntax, target);

        expect(result).toEqual(
            expect.objectContaining({
                title: expect.any(String),
                syntax: expect.any(String),
                fn: expect.any(Function),
            })
        );
    });

    it('should call getSnippetContent and return the snippet', () => {
        const target = 'java_unirest';
        const expectedResult =
            // eslint-disable-next-line no-useless-concat
            'HttpResponse<String> response = Unirest.get("http://undefinedundefined/path/to/api")\n' + '  .asString();';
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
            get: jest.fn(),
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };
        const snippet = getSnippetContent(req, target, []);
        expect(snippet).toEqual(expectedResult);
    });

    it('should call getSnippetContent and return the customized snippet', () => {
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
            get: jest.fn(),
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };
        const customizedSnippet = { endpoint: '/path/to/api', language: 'java', codeBlock: 'someCode;' };
        const snippet = getSnippetContent(req, target, customizedSnippet);
        expect(snippet).toEqual('someCode;');
    });

    it('should call getSnippetContent and return null when endpoint not matching the operation path', () => {
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
            get: jest.fn(),
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };
        const customizedSnippet = { endpoint: '/different/path', language: 'java', codeBlock: 'someCode;' };
        const snippet = getSnippetContent(req, target, customizedSnippet);
        expect(snippet).toEqual(null);
    });

    it('should catch error when trying to generate the snippet', () => {
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
            get: jest.fn(),
            toJS: () => ({
                spec,
                oasPathMethod,
            }),
        };
        const snippet = getSnippetContent(req, null, null);
        expect(snippet).toBe(undefined);
    });
});
