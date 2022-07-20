/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Typography, Tooltip } from '@material-ui/core';
import { Component } from 'react';
import './InstanceInfo.css';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class InstanceInfo extends Component {
    render() {
        const { selectedService, selectedVersion } = this.props;

        const apiInfo =
            selectedService.apis[selectedVersion || selectedService.defaultApiVersion] || selectedService.apis.default;
        const { apiId } = apiInfo;
        return (
            <Shield title="Cannot display information about selected instance">
                <div className="apiInfo-item">
                    <Tooltip key={selectedService.baseUrl} title="The instance URL for this service" placement="bottom">
                        <Typography>
                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                            <label htmlFor="instanceUrl">Instance URL:</label>
                            <span id="instanceUrl">{selectedService.baseUrl}</span>
                        </Typography>
                    </Tooltip>
                </div>
                <div className="apiInfo-item">
                    <Tooltip title="API IDs of the APIs that are provided by this service" placement="bottom">
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
