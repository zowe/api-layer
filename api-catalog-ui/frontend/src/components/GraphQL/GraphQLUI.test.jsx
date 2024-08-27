/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { getIntrospectionQuery } from 'graphql/utilities';
import { render, screen, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import GraphQLUI from './GraphQLUIApiml';

const host = 'localhost:3000';
const protocol = 'https:';
const graphqlUrl = 'https://localhost:4000/discoverableclient/api/v3/graphql';

const mockIntrospectionResponse = {
    data: {
        __schema: {
            queryType: { name: 'Query' },
            mutationType: null,
            subscriptionType: null,
            types: [
                {
                    kind: 'OBJECT',
                    name: 'Query',
                    fields: [
                        {
                            name: 'exampleField',
                            args: [],
                            type: {
                                kind: 'SCALAR',
                                name: 'String',
                            },
                            isDeprecated: false,
                            deprecationReason: null,
                        },
                    ],
                    interfaces: [],
                },
                {
                    kind: 'SCALAR',
                    name: 'String',
                },
            ],
            directives: [],
        },
    },
};

describe('>>> GraphQL component tests', () => {
    beforeAll(() => {
        delete window.location;
        window.location = {
            protocol,
            host,
        };

        // Mock document.createRange()
        document.createRange = () => {
            const range = new Range();
            range.getBoundingClientRect = jest.fn();
            range.getClientRects = jest.fn(() => ({
                item: () => null,
                length: 0,
            }));
            return range;
        };
    });

    function mockFetcher() {
        global.fetch = jest.fn();
        jest.mock('graphql/utilities', () => ({
            ...jest.requireActual('graphql/utilities'),
            buildClientSchema: jest.fn((data) => data),
        }));
        global.fetch.mockResolvedValueOnce({
            json: async () => mockIntrospectionResponse,
            text: async () => JSON.stringify(mockIntrospectionResponse),
            clone() {
                return this;
            },
        });
    }

    // function mockFetcherError() {
    //     global.fetch = jest.fn();
    //     jest.mock('graphql/utilities', () => ({
    //         ...jest.requireActual('graphql/utilities'),
    //         buildClientSchema: jest.fn((data) => data),
    //     }));
    //     global.fetch.mockRejectedValueOnce(new Error('Network Error'));
    // }

    it('should render the GraphiQL container', async () => {
        await act(async () => render(<GraphQLUI graphqlUrl={graphqlUrl} />));
        expect(screen.getByTestId('graphiql-container')).toBeInTheDocument();
    }, 10000);

    it('getUrl constructs the correct URL', async () => {
        mockFetcher();
        render(<GraphQLUI graphqlUrl={graphqlUrl} />);
        await waitFor(() => expect(fetch).toHaveBeenCalled());

        expect(fetch).toHaveBeenCalledWith(
            `${protocol}//${host}/discoverableclient/api/v3/graphql`,
            expect.objectContaining({
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: expect.any(String),
                credentials: 'same-origin',
            })
        );
    });

    it('Fetches and sets schema on mount', async () => {
        mockFetcher();

        render(<GraphQLUI graphqlUrl={graphqlUrl} />);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                `${protocol}//${host}/discoverableclient/api/v3/graphql`,
                expect.objectContaining({
                    method: 'POST',
                    body: JSON.stringify({ query: getIntrospectionQuery() }),
                })
            );
        });
    });

    it('should handle fetch failure and log error', async () => {
        // Mocking fetch to reject with an error
        global.fetch = jest.fn().mockRejectedValue(new Error('Network Error'));

        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        await act(async () => render(<GraphQLUI graphqlUrl={graphqlUrl} />));
        expect(consoleErrorSpy).toHaveBeenCalledWith('Error fetching data:', expect.any(Error));
        expect(screen.getByTestId('graphiql-container')).toBeInTheDocument();
        consoleErrorSpy.mockRestore();
    });

    it('should handle error while parsing response as JSON', async () => {
        // Mocking fetch to return a non-JSON response
        global.fetch = jest.fn().mockResolvedValue({
            json: async () => {
                throw new Error('Invalid JSON');
            },
            text: async () => 'Non-JSON response',
            clone() {
                return this;
            },
        });

        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        await act(async () => render(<GraphQLUI graphqlUrl={graphqlUrl} />));
        expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load GraphQL schema');
        consoleErrorSpy.mockRestore();
    });
});
