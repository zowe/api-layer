/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { useEffect, useRef, useState } from 'react';
import GraphiQL from 'graphiql';
import 'graphiql/graphiql.min.css';
import './GraphQLUIApiml.css';
import { buildClientSchema, getIntrospectionQuery } from 'graphql/utilities';

/**
 * Constructs the URL for the GraphQL endpoint by combining the protocol, host, and path.
 *
 * @param {string} graphqlUrl - The GraphQL endpoint URL.
 * @returns {string} The full URL for the GraphQL endpoint.
 */
export function getUrl(graphqlUrl) {
    const location = `${window.location.protocol}//${window.location.host}`;
    const urlForPathName = new URL(graphqlUrl);
    const pathName = urlForPathName.pathname;
    return `${location}${pathName}`;
}

/**
 * A functional component that renders a GraphiQL interface for interacting with a GraphQL API.
 *
 * @param {Object} props - The props for the component.
 * @param {string} props.graphqlUrl - The URL of the GraphQL API endpoint.
 *
 * @returns {JSX.Element} The GraphiQL interface component.
 */
export default function GraphQLUIApiml(props) {
    const basePath = getUrl(props.graphqlUrl);
    /**
     * Fetches data from the GraphQL endpoint.
     *
     * @param {Object} graphQLParams - The parameters for the GraphQL query.
     * @returns {Promise<Object>} The response data from the GraphQL API.
     */
    const fetcher = async (graphQLParams) => {
        try {
            const response = await fetch(basePath, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(graphQLParams),
                credentials: 'same-origin',
            });
            const data = response
                .clone()
                .json()
                .catch(() => response.text());
            return data;
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error('Error fetching data:', error);
            return { errors: [{ message: 'Failed to fetch data' }] };
        }
    };
    const [schema, setSchema] = useState(null);
    const graphiqlRef = useRef(null);

    useEffect(() => {
        /**
         * Fetches and sets the GraphQL schema using introspection.
         *
         * @async
         * @function fetchSchema
         */
        const fetchSchema = async () => {
            try {
                const introspectionQuery = getIntrospectionQuery();
                const result = await fetcher({ query: introspectionQuery });
                if (result?.data) {
                    const clientSchema = buildClientSchema(result.data);
                    setSchema(clientSchema);
                } else {
                    // eslint-disable-next-line no-console
                    console.error('Failed to load GraphQL schema');
                }
            } catch (error) {
                // eslint-disable-next-line no-console
                console.error('Error fetching GraphQL schema:', error);
            }
        };
        fetchSchema();
    }, [basePath]);

    useEffect(() => {
        /**
         * Updates the text of untitled tabs in the GraphiQL interface.
         *
         * @function updateTabText
         */
        const updateTabText = () => {
            const tabs = document.querySelectorAll('.graphiql-tab-button');
            tabs.forEach((tab, index) => {
                if (tab.textContent === '<untitled>') {
                    tab.textContent = `My Query ${index + 1}`;
                }
            });
        };
        updateTabText();

        /**
         * Observer to detect changes in the GraphiQL tab list and update tab text accordingly.
         *
         * @function
         * @param {MutationRecord[]} mutationsList - List of mutations observed.
         */
        const observer = new MutationObserver((mutationsList) => {
            mutationsList.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    updateTabText();
                }
            });
        });
        if (graphiqlRef.current) {
            observer.observe(graphiqlRef.current, {
                childList: true,
                subtree: true,
            });
        }
        return () => {
            observer.disconnect();
        };
    }, [graphiqlRef]);

    return (
        <div id="graphiql-container" ref={graphiqlRef}>
            <GraphiQL fetcher={fetcher} schema={schema} defaultQuery="# Write your query here!">
                <GraphiQL.Toolbar />
            </GraphiQL>
        </div>
    );
}
