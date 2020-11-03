import React, { Component } from 'react';
import SwaggerUi, { presets } from 'swagger-ui-react/swagger-ui';
import './Swagger.css';

export default class SwaggerUI extends Component {
    componentDidMount() {
        this.retrieveSwagger();
    }

    componentDidUpdate(prevProps) {
        const { selectedService, selectedVersion } = this.props;
        if (
            selectedService.serviceId !== prevProps.selectedService.serviceId ||
            selectedService.tileId !== prevProps.selectedService.tileId  ||
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
        const { selectedService, selectedVersion } = this.props;
        let swaggerConfig;
        try {
            if (
                selectedVersion == null &&
                selectedService.apiDoc !== null &&
                selectedService.apiDoc !== undefined &&
                selectedService.apiDoc.length !== 0
            ) {
                swaggerConfig = { 
                    dom_id: '#swaggerContainer',
                    spec: JSON.parse(selectedService.apiDoc),
                    presets: [presets.apis],
                    plugins: [this.customPlugins],
                };
            } else {
                const url = `${process.env.REACT_APP_GATEWAY_URL + 
                    process.env.REACT_APP_CATALOG_HOME + 
                    process.env.REACT_APP_APIDOC_UPDATE}/${selectedService.serviceId}/v2`;
                swaggerConfig = { 
                    dom_id: '#swaggerContainer',
                    url,
                    presets: [presets.apis],
                    plugins: [this.customPlugins],
                }
            }
            SwaggerUi(swaggerConfig);
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
                        <h4 style={{ color: '#de1b1b' }}>API documentation could not be retrieved. There may be something wrong in your Swagger definition. Please review the values of 'schemes', 'host' and 'basePath'.</h4>
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
