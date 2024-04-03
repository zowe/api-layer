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
import { ReactComponent as SwaggerIcon } from '../../assets/images/swagger.svg';
import { ReactComponent as UseCasesIcon } from '../../assets/images/usecases.svg';
import { ReactComponent as VideoIcon } from '../../assets/images/videos.svg';
import { ReactComponent as TutorialIcon } from '../../assets/images/tutorials.svg';
import utilFunctions, { isAPIPortal, findAndFormatZowe } from '../../utils/utilFunctions';

export default class Tile extends Component {
    handleClick = () => {
        const { tile, history, storeCurrentTileId, service } = this.props;
        const tileRoute = `/service/${service.serviceId}`;
        storeCurrentTileId(tile.id);
        history.push(tileRoute);
        localStorage.setItem('serviceId', service.serviceId);
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
            <Card key={tile.id} className="grid-tile pop grid-item" onClick={this.handleClick} data-testid="tile">
                <CardActionArea style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="card-action">
                    <CardContent style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="tile">
                        <div className="tile-ctn">
                            <div className="tile-title">
                                <Typography id="tiles-service-title" variant="subtitle1">
                                    {findAndFormatZowe(service.title)}
                                </Typography>
                                <span className="tile-desc">{service.description}</span>
                            </div>

                            <div className="icon-ctn desktop-view">
                                <div title="Swagger" className={hasSwagger ? '' : 'disabled-counter'}>
                                    <div className="icon-img-ctn">
                                        <SwaggerIcon
                                            onClick={() => this.goToExtraContents('#swagger-label', false)}
                                            className="swagger-icon"
                                            alt=""
                                        />
                                    </div>
                                </div>
                                <div className={useCasesCounter === 0 ? 'disabled-counter' : ''} title="Use Cases">
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
                                </div>
                                <div className={videosCounter === 0 ? 'disabled-counter' : ''} title="Videos">
                                    <div className="icon-img-ctn">
                                        <Typography
                                            className="media-labels"
                                            id="videos-counter"
                                            size="medium"
                                            variant="outlined"
                                            onClick={() => this.goToExtraContents('#videos-label', videosCounter === 0)}
                                        >
                                            {videosCounter}
                                        </Typography>
                                        <VideoIcon
                                            onClick={() => this.goToExtraContents('#videos-label', videosCounter === 0)}
                                            className="video-icon"
                                            alt=""
                                        />
                                    </div>
                                </div>
                                <div
                                    className={tutorialsCounter === 0 ? 'disabled-counter' : ''}
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
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </CardActionArea>
            </Card>
        );
    }
}
