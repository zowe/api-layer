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
import PropTypes from "prop-types";

function transformSwaggerToCurrentHost(swagger, service) {
    swagger.host = window.location.host;

    if (swagger.servers?.length) {
        swagger.servers.forEach((server) => {
            const location = `${window.location.protocol}//${window.location.host}`;
            try {
                const swaggerUrl = new URL(server.url);
                if (swaggerUrl?.pathname?.includes('gateway')) {
                    const basePath = service?.basePath === '/' ? '' : service?.basePath || '';

                    server.url = location + basePath + swaggerUrl.pathname;
                }
                else {
                    server.url = location + swaggerUrl.pathname;
                }
            } catch (e) {
                // not a proper url, assume it is an endpoint
                server.url = location + server.url;
            }
        });
    }
    return swagger;
}

function setFilterBarStyle() {
    const filterInput = document.getElementsByClassName('operation-filter-input');
    if (filterInput && filterInput.length > 0) {
        filterInput.item(0).placeholder = 'Search in endpoints...';
    }
}

export default class SwaggerUIApiml extends Component {
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
        setFilterBarStyle();
        const { service, selectedVersion } = this.props;
        if (
            service.serviceId !== prevProps.service.serviceId ||
            service.tileId !== prevProps.service.tileId ||
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
                const { service, selectedVersion, tiles } = this.props;

                return (
                    <div>
                        <InstanceInfo {...props} service={service} selectedVersion={selectedVersion} tiles={tiles} />
                        <Original {...props} />
                    </div>
                )
            },
        },
    });

    setSwaggerState = () => {
        const { service, selectedVersion } = this.props;
        let codeSnippets = null;
        if (service && 'apis' in service && service.apis && service.apis.length !== 0) {
            if (
                service.apis[selectedVersion] !== null &&
                service.apis[selectedVersion] !== undefined &&
                'codeSnippet' in service.apis[selectedVersion]
            ) {
                codeSnippets = service.apis[selectedVersion].codeSnippet;
            } else if (
                service.apis[service.defaultApiVersion] !== null &&
                service.apis[service.defaultApiVersion] !== undefined &&
                'codeSnippet' in service.apis[service.defaultApiVersion]
            ) {
                codeSnippets = service.apis[service.defaultApiVersion].codeSnippet;
            } else if (
                service.apis.default !== null &&
                service.apis.default !== undefined &&
                'codeSnippet' in service.apis.default
            ) {
                codeSnippets = service.apis.default.codeSnippet;
            }
        }
        try {
            // If no version selected use the default apiDoc
            if (
                (selectedVersion === null || selectedVersion === undefined) &&
                service?.apiDoc?.length
            ) {
                const swagger = transformSwaggerToCurrentHost(JSON.parse(service.apiDoc), service);

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
            if (selectedVersion && service) {
                const basePath = `${service.serviceId}/${selectedVersion}`;
                const url = `${getBaseUrl()}${process?.env.REACT_APP_APIDOC_UPDATE}/${basePath}`;
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
                            const swagger = transformSwaggerToCurrentHost(JSON.parse(res.text), service);
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
        const { service } = this.props;
        const { swaggerReady, swaggerProps } = this.state;
        let error = false;
        if (
            service.apiDoc === undefined ||
            service.apiDoc === null ||
            service.apiDoc.length === 0
        ) {
            error = true;
        }
        return (
            <div style={{ width: '100%', background: '#ffffff' }}>
                {error && (
                    <div style={{ width: '100%', background: '#ffffff', paddingLeft: 55 }}>
                        <h4 id="no-doc_message">
                            {service.apiDocErrorMessage
                                ? service.apiDocErrorMessage
                                : "API documentation could not be retrieved. There may be something wrong in your Swagger definition. Please review the values of 'schemes', 'host' and 'basePath'."}
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

SwaggerUIApiml.propTypes = {
    service: PropTypes.shape({
        apiDoc: PropTypes.string,
        apiDocErrorMessage: PropTypes.string,
    }).isRequired,
    url: PropTypes.string,
};

SwaggerUIApiml.defaultProps = {
    url: `${getBaseUrl()}/apidoc`,
};
