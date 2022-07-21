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

export function getSnippetContent(req, target, codeBlock) {
    // get extended info about request
    const { spec, oasPathMethod } = req.toJS();
    const { path, method } = oasPathMethod;
    // eslint-disable-next-line no-console
    console.log(path);
    // run OpenAPISnippet for target node
    const targets = [target];
    let snippet;
    try {
        // set request snippet content
        if (codeBlock !== undefined && codeBlock !== null) {
            snippet = codeBlock;
        } else {
            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets).snippets[0].content;
        }
    } catch (err) {
        snippet = JSON.stringify(snippet);
    }
    return snippet;
}

export function re(codeBlock) {
    return codeBlock;
}
/**
 * Generate the code snippets for each of the APIs
 * @param system
 * @param title the code snippet title
 * @param syntax the syntax used for indentation
 * @param target the language target
 * @returns snippet code snippet content or an error in case of failure
 */
export function generateSnippet(system, title, syntax, target, codeBlock) {
    return system.Im.fromJS({
        title,
        syntax,
        fn: (req) => getSnippetContent(req, target, codeBlock),
    });
}

/**
 * Custom Plugin which extends the SwaggerUI to generate simple snippets
 */
// eslint-disable-next-line import/prefer-default-export
export const BasicSnippedGenerator = {
    statePlugins: {
        // extend some internals to gain information about current path, method and spec in the generator function
        spec: wrapSelectors.spec,
        // extend the request snippets core plugin
        requestSnippets: {
            wrapSelectors: {
                // add additional snippet generators here
                getSnippetGenerators:
                    (ori, system) =>
                    (state, ...args) =>
                        ori(state, ...args)
                            .set('java_unirest', generateSnippet(system, 'Java Unirest', 'java', 'java_unirest', null))
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
                            .set('node_fetch', generateSnippet(system, 'NodeJS', 'javascript', 'node_fetch', null)),
            },
        },
    },
};

/**
 * Custom Plugin which extends the SwaggerUI to generate customized snippets
 */
export function CustomizedSnippedGenerator(codeSnippets) {
    const newCs = [];
    const cs = {
        codeBlock: 'System.out.println("ciao");',
        endpoint: '/gjgj',
        language: 'java',
        target: 'java_unirest',
    };
    const cs2 = { codeBlock: 'printf("ciao")', endpoint: '/aa', language: 'python', target: 'python' };
    newCs.push(cs);
    newCs.push(cs2);
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
                            let target;
                            // eslint-disable-next-line no-plusplus
                            for (let i = 0; i < codeSnippets.length; i++) {
                                switch (codeSnippets[i].language.toLowerCase()) {
                                    case 'java':
                                        target = 'java_unirest';
                                        break;
                                    case 'python':
                                        target = 'python';
                                        break;
                                    case 'javascript':
                                        target = 'javascript_jquery';
                                        break;
                                    case 'c':
                                        target = 'c_libcurl';
                                        break;
                                    case 'c#':
                                        target = 'csharp_restsharp';
                                        break;
                                    case 'go':
                                        target = 'go_native';
                                        break;
                                    case 'node':
                                        target = 'node_fetch';
                                        break;
                                    case 'nodejs':
                                        target = 'node_fetch';
                                        break;
                                    default:
                                        target = '';
                                        break;
                                }
                                const newSnippet = generateSnippet(
                                    system,
                                    `Customized Snippet - ${codeSnippets[i].language}`,
                                    codeSnippets[i].language,
                                    target,
                                    codeSnippets[i].codeBlock
                                );
                                // eslint-disable-next-line no-console
                                console.log(newSnippet);
                                useSet = useSet.set(codeSnippets[i].language, newSnippet);
                            }

                            return useSet;
                        },
                },
            },
        },
    };
}
