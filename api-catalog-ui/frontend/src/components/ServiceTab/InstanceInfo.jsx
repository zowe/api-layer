import {Text, Tooltip, ThemeProvider} from 'mineral-ui';
import React, {Component} from 'react';
import './InstanceInfo.css';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class InstanceInfo extends Component {

    render() {
        const {
            selectedService,
            selectedVersion
        } = this.props;

        let apiId = selectedService.apiId[selectedVersion || selectedService.defaultApiVersion] || selectedService.apiId["default"];
        return (
            <ThemeProvider>
                <Shield title="Cannot display information about selected instance">
                    <div className="apiInfo-item">
                        <Tooltip
                            key={selectedService.baseUrl}
                            content="The instance URL for this service"
                            placement="bottom"
                        >
                            <Text><label for="instanceUrl">Instance URL:</label><span id="instanceUrl">{selectedService.baseUrl}</span></Text>
                        </Tooltip>
                    </div>
                    <div className="apiInfo-item">
                        <Tooltip
                            key={selectedService.apiId}
                            content="API IDs of the APIs that are provided by this service"
                            placement="bottom"
                        >
                            <Text><label for="apiid">API ID:</label><span id="appid">{apiId}</span></Text>
                        </Tooltip>
                    </div>
                </Shield>
            </ThemeProvider>
        )
    }

}