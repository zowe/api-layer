import React, { Component } from 'react';
import SwaggerUi, { presets } from 'swagger-ui';
import './Swagger.css';

export default class SwaggerUI extends Component {
    componentDidMount() {
        this.retrieveSwagger();
    }

    componentDidUpdate(prevProps) {
        const { selectedService } = this.props;
        if (
            selectedService.serviceId !== prevProps.selectedService.serviceId ||
            selectedService.tileId !== prevProps.selectedService.tileId
        ) {
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

        if (
            selectedService.apiDoc !== null &&
            selectedService.apiDoc !== undefined &&
            selectedService.apiDoc.length !== 0
        ) {
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
                        <h4 style={{ color: '#de1b1b' }}>API documentation could not be retrieved. Please review the values of 'schemes', 'host' and 'basePath' in your Swagger definition.</h4>
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
