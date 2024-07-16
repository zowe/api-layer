/* eslint-disable */
import React, {useEffect, useRef, useState} from 'react';
import GraphiQL from 'graphiql';
import {buildClientSchema, getIntrospectionQuery} from 'graphql';
import getBaseUrl from '../../helpers/urls';
import 'graphiql/graphiql.min.css';


export default function GraphQLUIApiml(props) {

    const location = `${window.location.protocol}//${window.location.host}`;
    const findGraphqlUrl = (apis) => {
        for (const apiKey in apis) {
            if (apis.hasOwnProperty(apiKey) && apis[apiKey]) {
                const api = apis[apiKey];
                if (api.graphqlUrl) {
                    return api.graphqlUrl;
                }
            }
        }
        return null;
    };
    const graphqlUrl = new URL(findGraphqlUrl(props.selectedService.apis));
    const pathName = graphqlUrl.pathname
    const basePath = `${location}/${pathName}`;

    const fetcher = async (graphQLParams) => {
        const data = await fetch(
                basePath, {
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

    const [schema, setSchema] = useState(null);
    const [query, setQuery] = useState('# Write your query here!');
    const graphiqlRef = useRef("");

    // const executeQuery = async (graphQLParams) => {
    //     const response = await fetch(basePath, {
    //         method: 'POST',
    //         headers: {
    //             Accept: 'application/json',
    //             'Content-Type': 'application/json',
    //         },
    //         credentials: 'same-origin',
    //         body: JSON.stringify(graphQLParams),
    //     });
    //     return await response.json();
    // };

    // const updateSchema = async () => {
    //     try {
    //         const { data } = await executeQuery({ query: getIntrospectionQuery() });
    //         const schema = buildClientSchema(data);
    //         setSchema(schema);
    //     } catch (error) {
    //         console.error('Error occurred when updating the schema:');
    //         console.error(error);
    //     }
    // };

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

