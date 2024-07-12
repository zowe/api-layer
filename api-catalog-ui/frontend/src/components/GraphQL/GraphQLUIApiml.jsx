/* eslint-disable */
import React, {useEffect, useRef, useState} from 'react';
import GraphiQL from 'graphiql';
import {buildClientSchema, getIntrospectionQuery} from 'graphql';
import 'graphiql/graphiql.min.css';

const fetcher = async (graphQLParams) => {
    const data = await fetch(
        'https://localhost:10010/discoverableclient/api/v1/graphql', {
            //headers Post method
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',

            },
            body: JSON.stringify(graphQLParams),
            credentials: 'same-origin',
        },
    );
    return data.json()
        .catch(() => data.text());
};

export default function GraphQLUIApiml() {

    const [schema, setSchema] = useState(null);
    const [query, setQuery] = useState('# Write your query here!');
    const graphiqlRef = useRef("");

    const executeQuery = async (graphQLParams) => {
        const response = await fetch('https://localhost:10010/discoverableclient/api/v1/graphql', {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            credentials: 'same-origin',
            body: JSON.stringify(graphQLParams),
        });
        return await response.json();
    };

    const updateSchema = async () => {
        try {
            const { data } = await executeQuery({ query: getIntrospectionQuery() });
            const schema = buildClientSchema(data);
            setSchema(schema);
        } catch (error) {
            console.error('Error occurred when updating the schema:');
            console.error(error);
        }
    };

    useEffect(() => {
        updateSchema();
    }, );

        return (
            <div id="graphiql-container">
                <GraphiQL
                    ref={graphiqlRef}
                    fetcher={fetcher}
                    schema={schema}
                    query={query}
                    onEditQuery={setQuery}
                >
                    <GraphiQL.Toolbar>
                    </GraphiQL.Toolbar>
                </GraphiQL>
            </div>
        );
}

