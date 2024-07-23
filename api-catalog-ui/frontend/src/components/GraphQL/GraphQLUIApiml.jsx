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
import { buildClientSchema, getIntrospectionQuery } from 'graphql/utilities';

function getUrl(graphqlUrl) {
    const location = `${window.location.protocol}//${window.location.host}`;
    const urlForPathName = new URL(graphqlUrl);
    const pathName = urlForPathName.pathname;
    return `${location}${pathName}`;
}

export default function GraphQLUIApiml(props) {
    const basePath = getUrl(props.graphqlUrl);
    const fetcher = async (graphQLParams) => {
        const data = await fetch(basePath, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'omit',
        });
        return data.json().catch(() => data.text());
    };
    const [schema, setSchema] = useState(null);
    const [query, setQuery] = useState('# Write your query here!');
    const graphiqlRef = useRef('');

    useEffect(() => {
        const fetchSchema = async () => {
            const introspectionQuery = getIntrospectionQuery();
            const result = await fetcher({ query: introspectionQuery });
            if (result && result.data) {
                const clientSchema = buildClientSchema(result.data);
                setSchema(clientSchema);
            }
        };
        fetchSchema();
    }, [basePath]);

    return (
        <div id="graphiql-container">
            <GraphiQL ref={graphiqlRef} fetcher={fetcher} schema={schema} query={query} onEditQuery={setQuery}>
                <GraphiQL.Toolbar />
            </GraphiQL>
        </div>
    );
}
