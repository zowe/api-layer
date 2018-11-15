import React, { Component } from 'react';
import SwaggerUi, { presets } from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';

import './swagger.css';
import Spinner from '../Spinner/Spinner';

export default class SwaggerUI extends Component {
    state = {
        isLoading: false,
        loadingStatus: null,
    };

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
        const { url, spec, serviceId, version, service} = this.props;

        const apidocUrl = `${url}/${serviceId}/${version}`;

        const swagger = JSON.parse(service.apiDoc);
        SwaggerUi({
            dom_id: '#swaggerContainer',
            // url: apidocUrl,
            spec: swagger,
            presets: [presets.apis],
            plugins: [this.customPlugins],
        });
    };

    render() {
        const { isLoading, loadingStatus } = this.state;
        return (
            <div style={{ width: '100%', background: '#ffffff' }}>
                <Spinner isLoading={isLoading} />
                {loadingStatus === 'failed' && (
                    <div style={{ width: '100%', background: '#ffffff', paddingLeft: 120, paddingTop: 30 }}>
                        <h2 style={{ color: '#de1b1b' }}>Swagger Not Loaded</h2>
                        <h5 style={{ color: '#de1b1b' }}>See console for details</h5>
                    </div>
                )}
                <div id="swaggerContainer" data-testid="swagger"/>
            </div>
        );
    }
}

SwaggerUI.defaultProps = {
    url: `${process.env.REACT_APP_CATALOG_HOME}/apidoc`,
};
