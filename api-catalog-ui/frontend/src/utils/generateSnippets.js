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

const wrapSelectors = {
    spec: {
        wrapSelectors: {
            requestFor: (ori) => (state, path, method) =>
                ori(path, method)
                    ?.set('spec', state.get('json', {}))
                    ?.setIn(['oasPathMethod', 'path'], path)
                    ?.setIn(['oasPathMethod', 'method'], method),
            mutatedRequestFor: (ori) => (state, path, method) =>
                ori(path, method)
                    ?.set('spec', state.get('json', {}))
                    ?.setIn(['oasPathMethod', 'path'], path)
                    ?.setIn(['oasPathMethod', 'method'], method),
        },
    },
};

/**
 * Generate the code snippets for each of the APIs
 * @param system
 * @param title the code snippet title
 * @param syntax the syntax used for indentation
 * @param target the language target
 * @returns snippet code snippet content or an error in case of failure
 */
function generateSnippet(system, title, syntax, target) {
    return system.Im.fromJS({
        title,
        syntax,
        fn: (req) => {
            // get extended info about request
            const { spec, oasPathMethod } = req.toJS();
            const { path, method } = oasPathMethod;
            // run OpenAPISnippet for target node
            const targets = [target];
            let snippet;
            try {
                // set request snippet content
                snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets).snippets[0].content;
            } catch (err) {
                snippet = JSON.stringify(snippet);
            }
            return snippet;
        },
    });
}

/**
 * Custom Plugin which extends the SwaggerUI to generate simple snippets
 */
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
                            .set('java_unirest', generateSnippet(system, 'Java Unirest', 'java', 'java_unirest'))
                            .set(
                                'javascript_jquery',
                                generateSnippet(system, 'jQuery AJAX', 'javascript', 'javascript_jquery')
                            )
                            .set(
                                'javascript_xhr',
                                generateSnippet(system, 'Javascript XHR', 'javascript', 'javascript_xhr')
                            )
                            .set('python', generateSnippet(system, 'Python', 'python', 'python'))
                            .set('c_libcurl', generateSnippet(system, 'C (libcurl)', 'bash', 'c_libcurl'))
                            .set('csharp_restsharp', generateSnippet(system, 'C#', 'c#', 'csharp_restsharp'))
                            .set('go_native', generateSnippet(system, 'Go', 'bash', 'go_native'))
                            .set('node_fetch', generateSnippet(system, 'NodeJS', 'javascript', 'node_fetch')),
            },
        },
    },
};

/**
 * Custom Plugin which extends the SwaggerUI to generate hand-crafted snippets
 */
// TODO parametrize CustomizedSnippetGenerator to read data from the backend. Not ready for usage
export const CustomizedSnippetGenerator = {
    statePlugins: {
        spec: wrapSelectors.spec,
        requestSnippets: {
            wrapSelectors: {
                getSnippetGenerators:
                    (ori, system) =>
                    (state, ...args) =>
                        ori(state, ...args).set(
                            // the code language can be taken from config too (i.e. apiInfo[0].codeSnippet[0].language)
                            'java_unirest',
                            system.Im.fromJS({
                                title: 'Java',
                                syntax: 'java',
                                fn: (req) => {
                                    const { spec, oasPathMethod } = req.toJS();
                                    const { path, method } = oasPathMethod;
                                    const targets = ['java_unirest'];
                                    let snippet;
                                    try {
                                        // the code to be replace should be read from configuration (i.e. apiInfo[0].codeSnippet.codeBlock)
                                        const code =
                                            'HttpResponse<String> response = Unirest.get("https://localhost:3000/apicatalog/api/v1/containers")\n' +
                                            '  .header("Authorization", "Basic REPLACE_BASIC_AUTH")\n' +
                                            '  .asString();';
                                        // Code snippet defined in the configuration
                                        // this is a placeholder, it must be read from config (i.e. apiInfo[0].codeSnippet[0].endpoint) and propagated to the Catalog UI and the replace should be conditional
                                        if (path === '/apiMediationClient') {
                                            snippet = code;
                                        } else {
                                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets)
                                                .snippets[0].content;
                                            snippet = snippet.replace(snippet[0].content, code);
                                        }
                                    } catch (err) {
                                        snippet = JSON.stringify(snippet);
                                    }
                                    return snippet;
                                },
                            })
                        ),
            },
        },
    },
};
