/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Link, Typography, Tooltip, MenuItem, Select, Button, IconButton } from '@material-ui/core';
import { Component } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';
import { isAPIPortal } from '../../utils/utilFunctions';
import VideoWrapper from '../ExtraContents/VideoWrapper';
import BlogContainer from '../ExtraContents/BlogContainer';

export default class ServiceTab extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedVersion: null,
            previousVersion: null,
            isDialogOpen: false,
            displayVideosCount: 2,
            displayUseCasesCount: 2,
            displayBlogsCount: 2,
        };
        this.handleDialogClose = this.handleDialogClose.bind(this);
    }

    get containsVersion() {
        const { currentService } = this;
        return currentService && 'apiVersions' in currentService && currentService.apiVersions;
    }

    get basePath() {
        const { selectedService } = this.props;
        const { selectedVersion } = this.state;

        let basePath = '';
        if (selectedService.basePath) {
            const version = selectedVersion || selectedService.defaultApiVersion;
            let gatewayUrl = '';
            if (selectedService.apis && selectedService.apis[version] && selectedService.apis[version].gatewayUrl) {
                gatewayUrl = selectedService.apis[version].gatewayUrl;
            }
            // Take the first part of the basePath and then add the gatewayUrl
            basePath = `/${selectedService.serviceId}/${gatewayUrl}`;
        }
        return basePath;
    }

    get currentService() {
        let currentService = null;

        const {
            match: {
                params: { serviceId },
            },
            selectedService,
            selectedTile,
            selectService,
            currentTileId,
            tiles,
        } = this.props;
        if (tiles && tiles.length > 0 && tiles[0] && tiles[0].services) {
            tiles[0].services.forEach((service) => {
                if (service.serviceId === serviceId) {
                    if (service.serviceId !== selectedService.serviceId || selectedTile !== currentTileId) {
                        selectService(service, currentTileId);
                    }
                    currentService = service;
                }
            });
        }
        return currentService;
    }

    get hasHomepage() {
        const { selectedService } = this.props;
        return (
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0
        );
    }

    get apiVersions() {
        let apiVersions = [];

        const { selectedVersion } = this.state;
        const { currentService } = this;

        if (this.containsVersion) {
            apiVersions = currentService.apiVersions.map((version) => {
                // Pre select default version or if only one version exists select that
                let tabStyle = {};
                if (
                    selectedVersion === null &&
                    (currentService.defaultApiVersion === version || currentService.apiVersions.length === 1)
                ) {
                    tabStyle = { backgroundColor: '#fff' };
                }
                if (selectedVersion === version) {
                    tabStyle = { backgroundColor: '#fff' };
                }
                return (
                    <MenuItem
                        key={version}
                        onClick={() => {
                            this.setState({ selectedVersion: version });
                        }}
                        value={version}
                        style={tabStyle}
                        data-testid="version"
                    >
                        {version}
                    </MenuItem>
                );
            });
        }
        return apiVersions;
    }

    handleDialogOpen = (currentService) => {
        const { selectedVersion } = this.state;
        if (selectedVersion === null) {
            this.setState({ previousVersion: currentService.defaultApiVersion });
        } else {
            this.setState({ previousVersion: selectedVersion });
        }
        this.setState({
            isDialogOpen: true,
            selectedVersion: 'diff',
            previousVersion: selectedVersion ?? currentService.defaultApiVersion,
        });
    };

    handleDialogClose = () => {
        this.setState({ isDialogOpen: false, selectedVersion: null });
    };

    showMoreVideos = () => {
        const { videos } = this.props;
        this.setState((prevState) => ({ displayVideosCount: prevState.displayVideosCount + videos.length }));
    };

    showMoreBlogs = () => {
        const { tutorials } = this.props;
        this.setState((prevState) => ({ displayBlogsCount: prevState.displayBlogsCount + tutorials.length }));
    };

    showMoreUseCases = () => {
        const { useCases } = this.props;
        this.setState((prevState) => ({ displayUseCasesCount: prevState.displayUseCasesCount + useCases.length }));
    };

    render() {
        const {
            match: {
                params: { serviceId },
            },
            tiles,
            selectedService,
            useCases,
            tutorials,
            videos,
            useCasesCounter,
            tutorialsCounter,
            videosCounter,
            documentation,
        } = this.props;
        const { displayVideosCount, displayBlogsCount, displayUseCasesCount } = this.state;
        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error('No tile is selected.');
        }
        const { selectedVersion, isDialogOpen } = this.state;
        const { basePath } = this;
        const { currentService } = this;
        const { hasHomepage } = this;
        const { apiVersions } = this;
        const { containsVersion } = this;
        const message = 'The API documentation was retrieved but could not be displayed.';
        const sso = selectedService.ssoAllInstances ? 'supported' : 'not supported';
        const apiPortalEnabled = isAPIPortal();
        const additionalContentsPresent = useCasesCounter !== 0 || tutorialsCounter !== 0 || videosCounter !== 0;
        return (
            <>
                {currentService === null && (
                    <Typography id="no-tiles-error" variant="h4">
                        <p>The service ID "{serviceId}" does not match any registered service</p>
                    </Typography>
                )}
                <Shield title={message}>
                    <div className="serviceTab">
                        <div className="header">
                            {!apiPortalEnabled && (
                                <Typography id="service-title" data-testid="service" variant="h4">
                                    {selectedService.title}
                                </Typography>
                            )}
                            {hasHomepage && !apiPortalEnabled && (
                                <>
                                    {selectedService.status === 'UP' && (
                                        <Tooltip
                                            data-testid="tooltip"
                                            key={selectedService.serviceId}
                                            title="Open Service Homepage"
                                            placement="bottom"
                                        >
                                            <Link data-testid="link" href={selectedService.homePageUrl}>
                                                <strong>Service Homepage</strong>
                                            </Link>
                                        </Tooltip>
                                    )}
                                    {selectedService.status === 'DOWN' && (
                                        <Tooltip
                                            key={selectedService.serviceId}
                                            title="API Homepage navigation is disabled as the service is not running"
                                            placement="bottom"
                                        >
                                            <Link data-testid="red-homepage" variant="danger">
                                                <strong>Service Homepage</strong>
                                            </Link>
                                        </Tooltip>
                                    )}
                                </>
                            )}
                            {!apiPortalEnabled && (
                                <div className="apiInfo-item">
                                    <Tooltip
                                        key={basePath}
                                        title="The path used by the Gateway to access API endpoints. This can be used to identify a service in client tools like Zowe CLI and Zowe explorer."
                                        placement="bottom"
                                    >
                                        <Typography data-testid="base-path" variant="subtitle2">
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="apiBasePath">API Base Path:</label>
                                            <span id="apiBasePath">{basePath}</span>
                                        </Typography>
                                    </Tooltip>
                                    <Tooltip
                                        key={selectedService.serviceId}
                                        title="The identifier for this service"
                                        placement="bottom"
                                    >
                                        <Typography data-testid="service-id" variant="subtitle2">
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="serviceId">Service ID:</label>
                                            <span id="serviceId">{selectedService.serviceId}</span>
                                        </Typography>
                                    </Tooltip>
                                    <Tooltip
                                        key={selectedService.ssoAllInstances}
                                        title="All the instances of this service claim support of the SSO using Zowe API ML JWT tokens"
                                        placement="bottom"
                                    >
                                        <Typography data-testid="sso" variant="subtitle2">
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="sso">SSO:</label>
                                            <span id="sso">{sso}</span>
                                        </Typography>
                                    </Tooltip>
                                </div>
                            )}

                            <Typography data-testid="description" variant="subtitle2" style={{ color: 'black' }}>
                                {selectedService.description}
                            </Typography>
                            <br />
                            {isAPIPortal() && documentation?.label && documentation?.url && (
                                <Typography variant="subtitle2">
                                    To know more about the {selectedService.title} service, see the
                                    <Link
                                        rel="noopener noreferrer"
                                        target="_blank"
                                        style={{ color: '#0056B3' }}
                                        className="service-doc-link"
                                        href={documentation.url}
                                    >
                                        {documentation.label}
                                    </Link>
                                    .
                                </Typography>
                            )}
                            <Typography id="swagger-label" className="title1" size="medium" variant="outlined">
                                Swagger
                            </Typography>
                            {containsVersion && currentService && (
                                <Typography id="version-label" variant="subtitle2">
                                    Service ID and Version:
                                </Typography>
                            )}
                        </div>
                        {currentService && apiVersions?.length > 0 && (
                            <div id="version-div">
                                <Select
                                    disabled={apiVersions.length < 2}
                                    displayEmpty
                                    id="version-menu"
                                    style={
                                        apiVersions.length < 2
                                            ? { color: '#6b6868', opacity: '0.5' }
                                            : { backgroundColor: '#fff', color: '#0056B3' }
                                    }
                                    value={
                                        this.state.selectedVersion
                                            ? this.state.selectedVersion
                                            : currentService.defaultApiVersion
                                    }
                                    data-testid="version-menu"
                                    disableUnderline
                                >
                                    {apiVersions}
                                </Select>
                                <Button
                                    id="compare-button"
                                    disabled={apiVersions.length < 2}
                                    style={
                                        apiVersions.length < 2
                                            ? { backgroundColor: '#e4e4e4', color: '#6b6868', opacity: '0.5' }
                                            : { backgroundColor: '#fff', color: '#0056B3' }
                                    }
                                    onClick={() => this.handleDialogOpen(currentService)}
                                    key="diff"
                                >
                                    <Typography className="version-text">Compare API Versions</Typography>
                                </Button>
                            </div>
                        )}
                        {selectedVersion !== 'diff' && <SwaggerContainer selectedVersion={selectedVersion} />}
                        {selectedVersion === 'diff' && isDialogOpen && containsVersion && (
                            <ServiceVersionDiffContainer
                                selectedVersion={this.state.previousVersion}
                                handleDialog={this.handleDialogClose}
                                serviceId={selectedService.serviceId}
                                versions={currentService.apiVersions}
                                isDialogOpen={isDialogOpen}
                            />
                        )}
                        {isAPIPortal() && additionalContentsPresent && (
                            <div id="detail-footer">
                                <Typography
                                    className="footer-labels"
                                    id="use-cases-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    Use Cases ({useCasesCounter})
                                </Typography>
                                <br />
                                <br />
                                <div id="blogs-container">
                                    {useCases &&
                                        useCases
                                            .slice(0, displayUseCasesCount)
                                            .map((useCase) => (
                                                <BlogContainer mediumUser={useCase.user} mediumBlogUrl={useCase.url} />
                                            ))}
                                </div>
                                {useCasesCounter > 0 && displayUseCasesCount < useCases.length && (
                                    <IconButton
                                        id="more-content-button"
                                        className="button-link"
                                        onClick={this.showMoreUseCases}
                                    >
                                        Show all ({useCasesCounter} articles)
                                    </IconButton>
                                )}
                                <br />
                                <br />
                                <Typography
                                    className="footer-labels"
                                    id="tutorials-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    TechDocs Resources ({tutorialsCounter})
                                </Typography>
                                <br />
                                <div id="blogs-container">
                                    {tutorials &&
                                        tutorials
                                            .slice(0, displayBlogsCount)
                                            .map((tutorial) => (
                                                <BlogContainer
                                                    mediumUser={tutorial.user}
                                                    mediumBlogUrl={tutorial.url}
                                                />
                                            ))}
                                </div>
                                {tutorialsCounter > 0 && displayBlogsCount < tutorials.length && (
                                    <IconButton
                                        id="more-content-button"
                                        className="button-link"
                                        onClick={this.showMoreBlogs}
                                    >
                                        Show all ({tutorialsCounter} articles)
                                    </IconButton>
                                )}
                                <br />
                                <br />
                                <Typography
                                    className="footer-labels"
                                    id="videos-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    Videos ({videosCounter})
                                </Typography>
                                <br />
                                <div>
                                    {videos &&
                                        videos.slice(0, displayVideosCount).map((url) => <VideoWrapper url={url} />)}
                                </div>
                                {videosCounter > 0 && displayVideosCount < videos.length && (
                                    <IconButton
                                        id="more-content-button"
                                        className="button-link"
                                        onClick={this.showMoreVideos}
                                    >
                                        Show all ({videosCounter})
                                    </IconButton>
                                )}
                            </div>
                        )}
                    </div>
                </Shield>
            </>
        );
    }
}
