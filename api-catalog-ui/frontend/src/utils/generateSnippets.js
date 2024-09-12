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

export const wrapSelectors = {
    spec: {
        wrapSelectors: {
            requestFor: (ori) => (state, path, method) =>
                ori(path, method)
                    ?.set('spec', state.get('json', {}))
                    ?.setIn(['oasPathMethod', 'path'], path)
                    ?.setIn(['oasPathMethod', 'method'], method),
            // prettier-ignore
            // eslint-disable-next-line no-shadow
            mutatedRequestFor: (ori) => (state, path, method) => // NOSONAR
                ori(path, method)
                    ?.set('spec', state.get('json', {}))
                    ?.setIn(['oasPathMethod', 'path'], path)
                    ?.setIn(['oasPathMethod', 'method'], method),
        },
    },
};

export function getSnippetContent(req, target, codeSnippet) {
    // get extended info about request
    const { spec, oasPathMethod } = req?.toJS?.() ?? {};
    const { path, method } = oasPathMethod;
    // Include query parameters in the path for comparison
    const query = req?.get('query');
    const queryString = query ? `?${new URLSearchParams(query).toString()}` : '';
    const fullPath = path + queryString;
    let snippet;
    try {
        // set request snippet content
        if (
            codeSnippet !== null &&
            codeSnippet !== undefined &&
            codeSnippet.codeBlock !== undefined &&
            codeSnippet.codeBlock !== null
        ) {
            if (codeSnippet.endpoint === fullPath) {
                snippet = codeSnippet.codeBlock;
            } else {
                snippet = null;
            }
        } else {
            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, [target]).snippets[0].content;
        }
    } catch (err) {
        snippet = JSON.stringify(snippet);
    }
    return snippet;
}

/**
 * Generate the code snippets for each of the APIs
 * @param system
 * @param title the code snippet title
 * @param syntax the syntax used for indentation
 * @param target the language target
 * @param codeSnippet the code snippet
 * @returns codeSnippet the code snippet
 */
export function generateSnippet(system, title, syntax, target, codeSnippet) {
    return system.Im.fromJS({
        title,
        syntax,
        fn: (req) => getSnippetContent(req, target, codeSnippet),
    });
}

/**
 * Custom Plugin which extends the SwaggerUI to generate customized snippets
 */
export function CustomizedSnippedGenerator(codeSnippets) {
    return {
        statePlugins: {
            // extend some internals to gain information about current path, method and spec in the generator function
            spec: wrapSelectors.spec,
            // extend the request snippets core plugin
            requestSnippets: {
                wrapSelectors: {
                    getSnippetGenerators:
                        (ori, system) =>
                        (state, ...args) => {
                            let useSet = ori(state, ...args);
                            if (codeSnippets !== null && codeSnippets !== undefined) {
                                // eslint-disable-next-line no-restricted-syntax
                                for (const codeSnippet of codeSnippets) {
                                    const newSnippet = generateSnippet(
                                        system,
                                        `Customized Snippet - ${codeSnippet.language}`,
                                        codeSnippet.language,
                                        'target',
                                        codeSnippet
                                    );
                                    useSet = useSet.set(codeSnippet.endpoint + codeSnippet.language, newSnippet);
                                }
                            }
                            useSet = useSet
                                .set(
                                    'java_unirest',
                                    generateSnippet(system, 'Java Unirest', 'java', 'java_unirest', null)
                                )
                                .set(
                                    'javascript_jquery',
                                    generateSnippet(system, 'jQuery AJAX', 'javascript', 'javascript_jquery', null)
                                )
                                .set(
                                    'javascript_xhr',
                                    generateSnippet(system, 'Javascript XHR', 'javascript', 'javascript_xhr', null)
                                )
                                .set('python', generateSnippet(system, 'Python', 'python', 'python', null))
                                .set('c_libcurl', generateSnippet(system, 'C (libcurl)', 'bash', 'c_libcurl', null))
                                .set('csharp_restsharp', generateSnippet(system, 'C#', 'c#', 'csharp_restsharp', null))
                                .set('go_native', generateSnippet(system, 'Go', 'bash', 'go_native', null))
                                .set('node_fetch', generateSnippet(system, 'NodeJS', 'javascript', 'node_fetch', null));
                            return useSet;
                        },
                },
            },
        },
    };
}
