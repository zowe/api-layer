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
            credentials: 'same-origin',
        });
        return data
            .clone()
            .json()
            .catch(() => data.text());
    };
    const [schema, setSchema] = useState(null);
    const graphiqlRef = useRef(null);

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

    // rename default untitled tabs
    useEffect(() => {
        const updateTabText = () => {
            const tabs = document.querySelectorAll('.graphiql-tab-button');
            tabs.forEach((tab, index) => {
                if (tab.textContent === '<untitled>') {
                    tab.textContent = `My Query ${index + 1}`;
                }
            });
        };
        updateTabText();

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
