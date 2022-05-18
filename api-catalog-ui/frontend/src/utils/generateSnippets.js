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

export const requestSnippets = {
    generators: {
        java: {
            title: 'Java',
            syntax: 'java',
        },
    },
    defaultExpanded: true,
    languages: ['node'],
};
// Since swagger-ui-react was not configured to change the request snippets some workarounds required
// configuration will be added programatically
// Custom Plugin
export const SnippedGenerator = {
    statePlugins: {
        // extend some internals to gain information about current path, method and spec in the generator function metioned later
        spec: {
            wrapSelectors: {
                requestFor: (ori, system) => (state, path, method) => {
                    return ori(path, method)
                        ?.set('spec', state.get('json', {}))
                        ?.setIn(['oasPathMethod', 'path'], path)
                        ?.setIn(['oasPathMethod', 'method'], method);
                },
                mutatedRequestFor: (ori) => (state, path, method) => {
                    return ori(path, method)
                        ?.set('spec', state.get('json', {}))
                        ?.setIn(['oasPathMethod', 'path'], path)
                        ?.setIn(['oasPathMethod', 'method'], method);
                },
            },
        },
        // extend the request snippets core plugin
        requestSnippets: {
            wrapSelectors: {
                // add additional snippet generators here
                getSnippetGenerators:
                    (ori, system) =>
                    (state, ...args) =>
                        ori(state, ...args)
                            // add node native snippet generator
                            .set(
                                'java_okhttp',
                                system.Im.fromJS({
                                    title: 'Java',
                                    syntax: 'java',
                                    fn: (req) => {
                                        // get extended info about request
                                        const { spec, oasPathMethod } = req.toJS();
                                        const { path, method } = oasPathMethod;
                                        // run OpenAPISnippet for target node
                                        const targets = ['java'];
                                        let snippet;
                                        try {
                                            // set request snippet content
                                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets)
                                                .snippets[0].content;
                                        } catch (err) {
                                            // eslint-disable-next-line no-console
                                            console.log('error');
                                            // set to error in case it happens the npm package has some flaws
                                            snippet = JSON.stringify(snippet);
                                        }
                                        // return stringified snipped
                                        return snippet;
                                    },
                                })
                            )
                            .set(
                                'java_unirest',
                                system.Im.fromJS({
                                    title: 'Java2',
                                    syntax: 'java',
                                    fn: (req) => {
                                        // get extended info about request
                                        const { spec, oasPathMethod } = req.toJS();
                                        const { path, method } = oasPathMethod;
                                        // run OpenAPISnippet for target node
                                        const targets = ['java_unirest'];
                                        let snippet;
                                        try {
                                            // set request snippet content
                                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets)
                                                .snippets[0].content;
                                        } catch (err) {
                                            // eslint-disable-next-line no-console
                                            console.log('error');
                                            // set to error in case it happens the npm package has some flaws
                                            snippet = JSON.stringify(snippet);
                                        }
                                        /* eslint-disable no-console */
                                        console.log(snippet);
                                        // return stringified snipped
                                        return snippet;
                                    },
                                })
                            )
                            .set(
                                'python',
                                system.Im.fromJS({
                                    title: 'Python',
                                    syntax: 'python',
                                    fn: (req) => {
                                        // get extended info about request
                                        const { spec, oasPathMethod } = req.toJS();
                                        const { path, method } = oasPathMethod;
                                        // run OpenAPISnippet for target node
                                        const targets = ['python'];
                                        let snippet;
                                        try {
                                            // set request snippet content
                                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets)
                                                .snippets[0].content;
                                        } catch (err) {
                                            // eslint-disable-next-line no-console
                                            console.log('error');
                                            // set to error in case it happens the npm package has some flaws
                                            snippet = JSON.stringify(snippet);
                                        }
                                        // return stringified snipped
                                        return snippet;
                                    },
                                })
                            )
                            .set(
                                'node_fetch',
                                system.Im.fromJS({
                                    title: 'NodeJS',
                                    syntax: 'javascript',
                                    fn: (req) => {
                                        // get extended info about request
                                        const { spec, oasPathMethod } = req.toJS();
                                        const { path, method } = oasPathMethod;
                                        // run OpenAPISnippet for target node
                                        const targets = ['node_fetch'];
                                        let snippet;
                                        try {
                                            // set request snippet content
                                            snippet = OpenAPISnippet.getEndpointSnippets(spec, path, method, targets)
                                                .snippets[0].content;
                                        } catch (err) {
                                            // eslint-disable-next-line no-console
                                            console.log('error');
                                            // set to error in case it happens the npm package has some flaws
                                            snippet = JSON.stringify(snippet);
                                        }
                                        // return stringified snipped
                                        return snippet;
                                    },
                                })
                            ),
            },
        },
    },
};

// Since swagger-ui-react was not configured to change the request snippets some workarounds required
// configuration will be added programatically
// Custom Plugin
export const template = {
    statePlugins: {
        // extend some internals to gain information about current path, method and spec in the generator function metioned later
        spec: {
            wrapSelectors: {
                requestFor: (ori, system) => (state, path, method) => {
                    return ori(path, method)
                        ?.set('spec', state.get('json', {}))
                        ?.setIn(['oasPathMethod', 'path'], path)
                        ?.setIn(['oasPathMethod', 'method'], method);
                },
                mutatedRequestFor: (ori) => (state, path, method) => {
                    return ori(path, method)
                        ?.set('spec', state.get('json', {}))
                        ?.setIn(['oasPathMethod', 'path'], path)
                        ?.setIn(['oasPathMethod', 'method'], method);
                },
            },
        },
        // extend the request snippets core plugin
        requestSnippets: {
            wrapSelectors: {
                // add additional snippet generators here
                getSnippetGenerators:
                    (ori, system) =>
                    (state, ...args) =>
                        ori(state, ...args).set(
                            'java_okhttp',
                            system.Im.fromJS({
                                title: 'Java',
                                syntax: 'java',
                                fn: (req) => {
                                    // TODO read the snippet from the Swagger config
                                    // const code =
                                    //     'HttpResponse<String> response = Cooco.get("https://localhost:3000/apicatalog/api/v1/containers")\n' +
                                    //     '  .header("Authorization", "Basic REPLACE_BASIC_AUTH")\n' +
                                    //     '  .asString();';
                                    // return code;
                                },
                            })
                        ),
            },
        },
    },
};
