/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component } from 'react';
import * as React from 'react';
import * as OpenAPISnippet from 'openapi-snippet';
import SwaggerUi, { presets } from 'swagger-ui-react/swagger-ui';
import './Swagger.css';
import InstanceInfo from '../ServiceTab/InstanceInfo';
import getBaseUrl from '../../helpers/urls';

function transformSwaggerToCurrentHost(swagger) {
    swagger.host = window.location.host;

    if (swagger.servers !== null && swagger.servers !== undefined) {
        for (let i = 0; i < swagger.servers.length; i += 1) {
            const location = `${window.location.protocol}//${window.location.host}`;
            try {
                const swaggerUrl = new URL(swagger.servers[i].url);
                swagger.servers[i].url = location + swaggerUrl.pathname;
            } catch (e) {
                // not a proper url, assume it is an endpoint
                swagger.servers[i].url = location + swagger.servers[i];
            }
        }
    }

    return swagger;
}

export default class SwaggerUI extends Component {
    componentDidMount() {
        this.retrieveSwagger();
    }

    componentDidUpdate(prevProps) {
        const { selectedService, selectedVersion } = this.props;
        if (
            selectedService.serviceId !== prevProps.selectedService.serviceId ||
            selectedService.tileId !== prevProps.selectedService.tileId ||
            selectedVersion !== prevProps.selectedVersion
        ) {
            this.retrieveSwagger();
        }
    }

    // configuration will be added programatically
    snippedGenerator = () => ({
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
                                    'java',
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
                                                snippet = OpenAPISnippet.getEndpointSnippets(
                                                    spec,
                                                    path,
                                                    method,
                                                    targets
                                                ).snippets[0].content;
                                                // eslint-disable-next-line no-console
                                                console.log('ciao');
                                                // eslint-disable-next-line no-console
                                                console.log(snippet);
                                            } catch (err) {
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
    });

    customPlugins = () => ({
        statePlugins: {
            spec: {
                wrapSelectors: {
                    allowTryItOutFor: () => () => true,
                },
                wrapActions: {
                    updateLoadingStatus:
                        (ori) =>
                        (...args) => {
                            const [loadingStatus] = args;
                            // eslint-disable-next-line react/no-unused-state
                            this.setState({ isLoading: loadingStatus === 'loading' });
                            // eslint-disable-next-line react/no-unused-state
                            this.setState({ loadingStatus });
                            return ori(...args);
                        },
                },
            },
        },
        wrapComponents: {
            // prettier-ignore
            // eslint-disable-next-line no-shadow
            operations: (Original, { React }) => props => { // NOSONAR
                const { selectedService, selectedVersion } = this.props;
                return (
                    <div>
                        <InstanceInfo {...props} selectedService={selectedService} selectedVersion={selectedVersion} />
                        <Original {...props} />
                    </div>
                );
            },
        },
    });

    retrieveSwagger = () => {
        const { selectedService, selectedVersion } = this.props;
        try {
            // If no version selected use the default apiDoc
            if (
                (selectedVersion === null || selectedVersion === undefined) &&
                selectedService.apiDoc !== null &&
                selectedService.apiDoc !== undefined &&
                selectedService.apiDoc.length !== 0
            ) {
                const swagger = transformSwaggerToCurrentHost(JSON.parse(selectedService.apiDoc));

                SwaggerUi({
                    dom_id: '#swaggerContainer',
                    spec: swagger,
                    presets: [presets.apis],
                    plugins: [this.customPlugins, this.snippedGenerator],
                });
            }
            if (selectedVersion !== null && selectedVersion !== undefined) {
                const basePath = `${selectedService.serviceId}/${selectedVersion}`;
                const url = `${getBaseUrl()}${process.env.REACT_APP_APIDOC_UPDATE}/${basePath}`;
                SwaggerUi({
                    dom_id: '#swaggerContainer',
                    url,
                    presets: [presets.apis],
                    plugins: [this.customPlugins, this.snippedGenerator],
                    responseInterceptor: (res) => {
                        // response.text field is used to render the swagger
                        const swagger = transformSwaggerToCurrentHost(JSON.parse(res.text));
                        res.text = JSON.stringify(swagger);
                        return res;
                    },
                });
            }
        } catch (e) {
            throw new Error(e);
        }
    };

    render() {
        const { selectedService } = this.props;
        let error = false;
        if (
            selectedService.apiDoc === undefined ||
            selectedService.apiDoc === null ||
            selectedService.apiDoc.length === 0
        ) {
            error = true;
        }
        return (
            <div style={{ width: '100%', background: '#ffffff' }}>
                {error && (
                    <div style={{ width: '100%', background: '#ffffff', paddingLeft: 55 }}>
                        <h4 style={{ color: '#de1b1b' }}>
                            API documentation could not be retrieved. There may be something wrong in your Swagger
                            definition. Please review the values of 'schemes', 'host' and 'basePath'.
                        </h4>
                    </div>
                )}
                {!error && <div id="swaggerContainer" data-testid="swagger" />}
            </div>
        );
    }
}

SwaggerUI.defaultProps = {
    url: `${getBaseUrl()}/apidoc`,
};
