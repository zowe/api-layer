/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Card, CardActionArea, CardContent, Link, Typography, Button } from '@material-ui/core';
import React, { Component } from 'react';
import Brightness1RoundedIcon from '@material-ui/icons/Brightness1Rounded';
import ReportProblemIcon from '@material-ui/icons/ReportProblem';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';
import { ReactComponent as SwaggerIcon } from '../../assets/images/swagger.svg';
import { ReactComponent as UseCasesIcon } from '../../assets/images/usecases.svg';
import { ReactComponent as VideoIcon } from '../../assets/images/videos.svg';
import { ReactComponent as TutorialIcon } from '../../assets/images/tutorials.svg';
import utilFunctions, { isAPIPortal, findAndFormatZowe } from '../../utils/utilFunctions';

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
        const apiPortalEnabled = isAPIPortal();
        if (!apiPortalEnabled) {
            const { status } = tile;
            switch (status) {
                case 'UP':
                    return 'The service is running';
                case 'DOWN':
                    return 'The service is not running';
                default:
                    return 'Status unknown';
            }
        }
    };

    handleClick = () => {
        const { tile, history, storeCurrentTileId, service } = this.props;
        const tileRoute = `/service/${service.serviceId}`;
        storeCurrentTileId(tile.id);
        history.push(tileRoute);
        localStorage.setItem('serviceId', service.serviceId);
    };

    showDesc = (e) => {
        e.target.closest('.grid-item')?.classList.toggle('expanded');
    };

    goToExtraContents = (id, flag) => {
        if (!flag) {
            const { storeContentAnchor } = this.props;
            storeContentAnchor(id);
            this.handleClick();
        }
    };

    render() {
        const { tile, service } = this.props;
        const apiPortalEnabled = isAPIPortal();
        const { useCasesCounter, tutorialsCounter, videosCounter, hasSwagger } = utilFunctions(service);

        return (
            <Card key={tile.id} className="grid-tile pop grid-item" onClick={this.showDesc} data-testid="tile">
                <CardActionArea style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="card-action">
                    <CardContent style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="tile">
                        <div className="tile-ctn">
                            <div className="tile-title">
                                <Typography id="tileLabel" className="grid-tile-status">
                                    {!apiPortalEnabled && this.getTileStatus(tile)}
                                    {!apiPortalEnabled && this.getTileStatusText(tile)}
                                </Typography>
                                <Typography id="tiles-service-title" variant="subtitle1">
                                    {findAndFormatZowe(service.title)}
                                </Typography>
                                {!apiPortalEnabled && service.sso && (
                                    <Typography variant="h6" id="grid-tile-sso">
                                        (SSO)
                                    </Typography>
                                )}
                            </div>

                            <div className="tile-desc">{service.description}</div>

                            {apiPortalEnabled && (
                                <div className="icon-ctn">
                                    <div className="expanded-spacer" />
                                    <div
                                        id="swagger"
                                        title="Swagger"
                                        className={hasSwagger ? 'link-counter' : 'disabled-counter'}
                                    >
                                        <Button className="icon-img-ctn" onClick={this.handleClick}>
                                            <SwaggerIcon className="icon-img" alt="" />
                                        </Button>
                                        <Link to="" onClick={this.handleClick} className="expanded-icon-title">
                                            Swagger
                                        </Link>
                                    </div>
                                    <div
                                        className={
                                            useCasesCounter === 0 ? 'disabled-counter desktop-view' : 'desktop-view'
                                        }
                                        title="Use Cases"
                                    >
                                        <div className="icon-img-ctn">
                                            <Typography
                                                className="media-labels"
                                                id="use-cases-counter"
                                                size="medium"
                                                variant="outlined"
                                                onClick={() =>
                                                    this.goToExtraContents('#use-cases-label', useCasesCounter === 0)
                                                }
                                            />
                                            <UseCasesIcon
                                                onClick={() =>
                                                    this.goToExtraContents('#use-cases-label', useCasesCounter === 0)
                                                }
                                                className="usecases-icon"
                                                alt=""
                                            />
                                        </div>
                                        <span className="expanded-icon-title">Use Cases</span>
                                    </div>
                                    <div
                                        className={
                                            videosCounter === 0 ? 'disabled-counter desktop-view' : 'desktop-view'
                                        }
                                        title="Videos"
                                    >
                                        <div className="icon-img-ctn">
                                            <Typography
                                                className="media-labels"
                                                id="videos-counter"
                                                size="medium"
                                                variant="outlined"
                                                onClick={() =>
                                                    this.goToExtraContents('#videos-label', videosCounter === 0)
                                                }
                                            >
                                                {videosCounter}
                                            </Typography>
                                            <VideoIcon
                                                onClick={() =>
                                                    this.goToExtraContents('#videos-label', videosCounter === 0)
                                                }
                                                className="video-icon"
                                                alt=""
                                            />
                                        </div>

                                        <span className="expanded-icon-title">Videos</span>
                                    </div>
                                    <div
                                        className={
                                            tutorialsCounter === 0 ? 'disabled-counter desktop-view' : 'desktop-view'
                                        }
                                        title="Getting Started"
                                    >
                                        <div className="icon-img-ctn">
                                            <Typography
                                                className="media-labels"
                                                id="tutorials-counter"
                                                size="medium"
                                                variant="outlined"
                                                onClick={() =>
                                                    this.goToExtraContents('#tutorials-label', tutorialsCounter === 0)
                                                }
                                            >
                                                {tutorialsCounter}
                                            </Typography>
                                            <TutorialIcon
                                                onClick={() =>
                                                    this.goToExtraContents('#tutorials-label', tutorialsCounter === 0)
                                                }
                                                className="tutorial-icon"
                                                alt=""
                                            />
                                        </div>
                                        <span className="expanded-icon-title">Getting Started</span>
                                    </div>
                                </div>
                            )}
                        </div>
                    </CardContent>
                </CardActionArea>
            </Card>
        );
    }
}
