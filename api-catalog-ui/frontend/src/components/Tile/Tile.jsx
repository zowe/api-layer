/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Card, CardActionArea, CardContent, Typography } from '@material-ui/core';
import React, { Component } from 'react';
import Brightness1RoundedIcon from '@material-ui/icons/Brightness1Rounded';
import ReportProblemIcon from '@material-ui/icons/ReportProblem';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';
import PropTypes from 'prop-types';

export default class Tile extends Component {
    getTileStatus = (tile) => {
        const unknownIcon = <HelpOutlineIcon id="unknown" style={{ color: 'rgb(51, 56, 64)', fontSize: '12px' }} />;
        if (tile === null || tile === undefined) {
            return unknownIcon;
        }
        const { status } = tile;
        switch (status) {
            case 'UP':
                return <Brightness1RoundedIcon id="success" style={{ color: 'rgb(42, 133, 78)', fontSize: '12px' }} />;
            case 'DOWN':
                return <ReportProblemIcon id="danger" style={{ color: 'rgb(222, 27, 27)', fontSize: '12px' }} />;
            default:
                return unknownIcon;
        }
    };

    getTileStatusText = (tile) => {
        if (tile === null || tile === undefined) {
            return 'Status unknown';
        }
        const { status } = tile;
        switch (status) {
            case 'UP':
                return 'The service is running';
            case 'DOWN':
                return 'The service is not running';
            default:
                return 'Status unknown';
        }
    };

    handleClick = () => {
        const { tile, history, storeCurrentTileId, service } = this.props;
        const tileRoute = `/service/${service.serviceId}`;
        storeCurrentTileId(tile.id);
        history.push(tileRoute);
        localStorage.setItem('serviceId', service.serviceId);
    };

    render() {
        const { tile, service } = this.props;
        return (
            <Card key={tile.id} className="grid-tile pop grid-item" onClick={this.handleClick} data-testid="tile">
                <CardActionArea style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="card-action">
                    <CardContent style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="tile">
                        <div className="tile-ctn">
                            <div className="tile-title">
                                <Typography id="tileLabel" className="grid-tile-status">
                                    {this.getTileStatus(tile)}
                                    {this.getTileStatusText(tile)}
                                </Typography>
                                <Typography id="tiles-service-title" variant="subtitle1">
                                    {service.title}
                                </Typography>
                                {service.sso && (
                                    <Typography variant="h6" id="grid-tile-sso">
                                        (SSO)
                                    </Typography>
                                )}
                            </div>
                        </div>
                    </CardContent>
                </CardActionArea>
            </Card>
        );
    }
}

Tile.propTypes = {
    service: PropTypes.shape({
        title: PropTypes.string,
        sso: PropTypes.bool,
    }).isRequired,
};
