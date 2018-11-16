import { Text } from 'mineral-ui';
import React, { Component } from 'react';
import SwaggerUi, { presets } from 'swagger-ui';
import './Swagger.css';

export default class SwaggerUI extends Component {

    componentDidMount() {
        this.retrieveSwagger();
    }

    componentDidUpdate(prevProps) {
        const { selectedService } = this.props;
        if (selectedService.serviceId !== prevProps.selectedService.serviceId || selectedService.tileId !== prevProps.selectedService.tileId) {
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
        const { selectedService } = this.props;

        if (selectedService.apiDoc !== null && selectedService.apiDoc !== undefined && selectedService.apiDoc.length !== 0) {
            try {
                const swagger = JSON.parse(selectedService.apiDoc);
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
        const { selectedService } = this.props;
        let error = false;
        if (selectedService.apiDoc === undefined || selectedService.apiDoc === null || selectedService.apiDoc.length === 0) {
            error = true;
        }
        return (
            <div>
                {error && (
                    <Text element="h3" color="#de1b1b" fontWeight="bold"
                          style={{ margin: '0 auto', 'background': '#ffff', width: '100%' }}>
                        <p style={{ marginLeft: '55px', marginTop: '50px' }}>Api Documentation for
                            service {selectedService.title}({selectedService.serviceId}) could not be retrieved or is
                            not defined.</p>
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
