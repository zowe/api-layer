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
import SwaggerUiReact from 'swagger-ui-react';
import './Swagger.css';
import { Buffer } from 'buffer';
import InstanceInfo from '../ServiceTab/InstanceInfo';

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
        window.Buffer = window.Buffer || Buffer;
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

                SwaggerUiReact({
                    dom_id: '#swaggerContainer',
                    spec: swagger,
                    presets: [SwaggerUiReact.presets.apis],
                    plugins: [this.customPlugins],
                });
            }
            if (selectedVersion !== null && selectedVersion !== undefined) {
                const url = `${
                    process.env.REACT_APP_GATEWAY_URL +
                    process.env.REACT_APP_CATALOG_HOME +
                    process.env.REACT_APP_APIDOC_UPDATE
                }/${selectedService.serviceId}/${selectedVersion}`;
                SwaggerUiReact({
                    dom_id: '#swaggerContainer',
                    url,
                    presets: [SwaggerUiReact.presets.apis],
                    plugins: [this.customPlugins],
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
    url: `${process.env.REACT_APP_CATALOG_HOME}/apidoc`,
};
