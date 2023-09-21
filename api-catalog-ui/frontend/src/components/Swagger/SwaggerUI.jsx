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
import SwaggerUi from 'swagger-ui-react';
import InstanceInfo from '../ServiceTab/InstanceInfo';
import getBaseUrl from '../../helpers/urls';
import { CustomizedSnippedGenerator } from '../../utils/generateSnippets';
import { AdvancedFilterPlugin } from '../../utils/filterApis';
import { isAPIPortal } from '../../utils/utilFunctions';

function transformSwaggerToCurrentHost(swagger) {
    swagger.host = window.location.host;

    if (swagger.servers !== null && swagger.servers !== undefined) {
        swagger.servers.forEach((server) => {
            const location = `${window.location.protocol}//${window.location.host}`;
            try {
                const swaggerUrl = new URL(server.url);
                server.url = location + swaggerUrl.pathname;
            } catch (e) {
                // not a proper url, assume it is an endpoint
                server.url = location + server;
            }
        });
    }

    return swagger;
}

export default class SwaggerUI extends Component {
    constructor(props) {
        super(props);
        this.state = {
            swaggerReady: false,
            swaggerProps: {},
        };
    }

    componentDidMount() {
        this.setSwaggerState();
    }

    componentDidUpdate(prevProps) {
        const { selectedService, selectedVersion } = this.props;
        if (
            selectedService.serviceId !== prevProps.selectedService.serviceId ||
            selectedService.tileId !== prevProps.selectedService.tileId ||
            selectedVersion !== prevProps.selectedVersion
        ) {
            this.setSwaggerState();
        }
    }

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
            // eslint-disable-next-line no-shadow, react/no-unstable-nested-components
            operations: (Original, { React }) => props => { // NOSONAR
                const { selectedService, selectedVersion, tiles } = this.props;

                return (
                    <div>
                        {!isAPIPortal() &&
                        (
                            <InstanceInfo {...props} selectedService={selectedService} selectedVersion={selectedVersion} tiles={tiles} />
                        )
                        }
                        <Original {...props} />
                    </div>
                )
            },
        },
    });

    setSwaggerState = () => {
        const { selectedService, selectedVersion } = this.props;
        let codeSnippets = null;
        if (selectedService && 'apis' in selectedService && selectedService.apis && selectedService.apis.length !== 0) {
            if (
                selectedService.apis[selectedVersion] !== null &&
                selectedService.apis[selectedVersion] !== undefined &&
                'codeSnippet' in selectedService.apis[selectedVersion]
            ) {
                codeSnippets = selectedService.apis[selectedVersion].codeSnippet;
            } else if (
                selectedService.apis[selectedService.defaultApiVersion] !== null &&
                selectedService.apis[selectedService.defaultApiVersion] !== undefined &&
                'codeSnippet' in selectedService.apis[selectedService.defaultApiVersion]
            ) {
                codeSnippets = selectedService.apis[selectedService.defaultApiVersion].codeSnippet;
            } else if (
                selectedService.apis.default !== null &&
                selectedService.apis.default !== undefined &&
                'codeSnippet' in selectedService.apis.default
            ) {
                codeSnippets = selectedService.apis.default.codeSnippet;
            }
        }
        try {
            // If no version selected use the default apiDoc
            if (
                (selectedVersion === null || selectedVersion === undefined) &&
                selectedService.apiDoc !== null &&
                selectedService.apiDoc !== undefined &&
                selectedService.apiDoc.length !== 0
            ) {
                const swagger = transformSwaggerToCurrentHost(JSON.parse(selectedService.apiDoc));

                this.setState({
                    swaggerReady: true,
                    swaggerProps: {
                        dom_id: '#swaggerContainer',
                        spec: swagger,
                        presets: [SwaggerUi.presets.apis],
                        requestSnippetsEnabled: true,
                        plugins: [this.customPlugins, AdvancedFilterPlugin, CustomizedSnippedGenerator(codeSnippets)],
                        filter: true,
                    },
                });
            }
            if (selectedVersion !== null && selectedVersion !== undefined) {
                const basePath = `${selectedService.serviceId}/${selectedVersion}`;
                const url = `${getBaseUrl()}${process.env.REACT_APP_APIDOC_UPDATE}/${basePath}`;
                this.setState({
                    swaggerReady: true,
                    swaggerProps: {
                        dom_id: '#swaggerContainer',
                        url,
                        presets: [SwaggerUi.presets.apis],
                        requestSnippetsEnabled: true,
                        plugins: [this.customPlugins, AdvancedFilterPlugin, CustomizedSnippedGenerator(codeSnippets)],
                        responseInterceptor: (res) => {
                            // response.text field is used to render the swagger
                            const swagger = transformSwaggerToCurrentHost(JSON.parse(res.text));
                            res.text = JSON.stringify(swagger);
                            return res;
                        },
                    },
                });
            }
        } catch (e) {
            throw new Error(e);
        }
    };

    render() {
        const { selectedService } = this.props;
        const { swaggerReady, swaggerProps } = this.state;
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
                        <h4 id="no-doc_message">
                            API documentation could not be retrieved. There may be something wrong in your Swagger
                            definition. Please review the values of 'schemes', 'host' and 'basePath'.
                        </h4>
                    </div>
                )}
                {!error && swaggerReady && (
                    <div id="swaggerContainer" data-testid="swagger">
                        <SwaggerUi {...swaggerProps} />
                    </div>
                )}
            </div>
        );
    }
}

SwaggerUI.defaultProps = {
    url: `${getBaseUrl()}/apidoc`,
};
