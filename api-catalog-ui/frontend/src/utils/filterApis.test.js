/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { extendFilter } from './filterApis';

describe('>>> Filter APIs', () => {
    it('should filter the operation', () => {
        const system = {
            Im: {
                fromJS: (obj) => obj,
            },
        };
        const spec = {
            'API Catalog': {
                tagDetails: {
                    name: 'API Catalog',
                    description: 'Api Catalog Controller',
                },
                operations: [
                    {
                        path: '/containers',
                        method: 'get',
                        operation: {
                            tags: ['API Catalog'],
                            summary: 'Lists catalog dashboard tiles',
                            description: 'Returns a list of tiles including status and tile description',
                            operationId: 'getAllAPIContainersUsingGET',
                            produces: ['application/json'],
                            parameters: [],
                            responses: {
                                200: {
                                    description: 'OK',
                                    schema: {
                                        type: 'array',
                                        items: {
                                            $ref: '#/definitions/APIContainer',
                                        },
                                    },
                                },
                            },
                        },
                        id: 'get-/containers',
                    },
                    {
                        path: '/containers/{id}',
                        method: 'get',
                        operation: {
                            tags: ['API Catalog'],
                            summary: 'Retrieves a specific dashboard tile information',
                            description:
                                'Returns information for a specific tile {id} including status and tile description',
                            operationId: 'getAPIContainerByIdUsingGET',
                            parameters: [
                                {
                                    name: 'id',
                                },
                            ],
                            responses: {
                                200: {
                                    description: 'OK',
                                    schema: {
                                        type: 'array',
                                        items: {
                                            $ref: '#/definitions/APIContainer',
                                        },
                                    },
                                },
                            },
                        },
                        id: 'get-/containers/{id}',
                    },
                ],
            },
        };
        const filteredApi = extendFilter('Lists catalog', spec, system);
        expect(filteredApi['API Catalog'].operations[0].operation.summary).toEqual('Lists catalog dashboard tiles');
    });
});
