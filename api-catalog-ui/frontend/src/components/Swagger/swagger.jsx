import React, { Component } from 'react';
import SwaggerUi, { presets } from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';

import './swagger.css';
import { Text } from "mineral-ui";

export default class SwaggerUI extends Component {

    componentDidMount() {
        this.retrieveSwagger();
    }

    componentDidUpdate(prevProps) {
        const { serviceId, version } = this.props;
        if (serviceId !== prevProps.serviceId || version !== prevProps.version) {
            this.retrieveSwagger();
        }
    }

    customPlugins = () => ({
        statePlugins: {
            spec: {
                wrapSelectors: {
                    allowTryItOutFor: () => () => false,
                },
                wrapActions: {
                    updateLoadingStatus: ori => (...args) => {
                        const [loadingStatus] = args;
                        this.setState({ isLoading: loadingStatus === 'loading' });
                        this.setState({ loadingStatus });
                        return ori(...args);
                    },
                },
            },
        },
    });

    retrieveSwagger = () => {
        const { service } = this.props;

        if (service.apiDoc !== null && service.apiDoc !== undefined && service.apiDoc.length !== 0) {
            try {
                const swagger = JSON.parse(service.apiDoc);
                SwaggerUi({
                    dom_id: '#swaggerContainer',
                    spec: swagger,
                    presets: [presets.apis],
                    plugins: [this.customPlugins],
                });
            } catch (e) {
                throw new Error(e);
            }
        }
    };

    render() {
        const { service } = this.props;
        let error = false;
        if (service.apiDoc === undefined || service.apiDoc === null || service.apiDoc.length === 0) {
            error = true;
        }
        return (
            <div>
            {error &&  (
                <Text element="h3" color="#de1b1b" fontWeight="bold" style={{ margin: '0 auto', 'background': '#ffff', width: '100%' }}>
                    <p style={{marginLeft: '55px', marginTop: '50px'}}>Api Documentation for this service could not be retrieved or is not defined.</p>
                </Text>
                )}
            {!error && (
                    <div id="swaggerContainer" data-testid="swagger"/>
            )}
            </div>
        );
    }
}

SwaggerUI.defaultProps = {
    url: `${process.env.REACT_APP_CATALOG_HOME}/apidoc`,
};
