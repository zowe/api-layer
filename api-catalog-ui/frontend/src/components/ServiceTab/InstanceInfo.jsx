import { Typography, Tooltip } from '@material-ui/core';
import { Component } from 'react';
import './InstanceInfo.css';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class InstanceInfo extends Component {
    render() {
        const { selectedService, selectedVersion } = this.props;

        const apiId =
            selectedService.apiId[selectedVersion || selectedService.defaultApiVersion] ||
            selectedService.apiId.default;
        return (
            <Shield title="Cannot display information about selected instance">
                <div className="apiInfo-item">
                    <Tooltip
                        key={selectedService.baseUrl}
                        content="The instance URL for this service"
                        placement="bottom"
                    >
                        <Typography>
                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                            <label htmlFor="instanceUrl">Instance URL:</label>
                            <span id="instanceUrl">{selectedService.baseUrl}</span>
                        </Typography>
                    </Tooltip>
                </div>
                <div className="apiInfo-item">
                    <Tooltip
                        key={selectedService.apiId}
                        content="API IDs of the APIs that are provided by this service"
                        placement="bottom"
                    >
                        <Typography>
                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                            <label htmlFor="apiid">API ID:</label>
                            <span id="appid">{apiId}</span>
                        </Typography>
                    </Tooltip>
                </div>
            </Shield>
        );
    }
}
