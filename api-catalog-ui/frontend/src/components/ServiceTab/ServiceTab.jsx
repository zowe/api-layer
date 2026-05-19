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
import PropTypes from 'prop-types';
import { useState, useCallback, useMemo } from 'react';
import { useParams } from 'react-router';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';
import { isAPIPortal } from '../../utils/utilFunctions';
import VideoWrapper from '../ExtraContents/VideoWrapper';
import BlogContainer from '../ExtraContents/BlogContainer';

export default function ServiceTab(props) {
    const { serviceId: urlServiceId } = useParams();
    const {
        tiles,
        selectedService,
        useCases,
        tutorials,
        videos,
        useCasesCounter,
        tutorialsCounter,
        videosCounter,
        documentation,
        selectService,
        currentTileId,
        fetchTilesStop,
    } = props;

    const [selectedVersion, setSelectedVersion] = useState(null);
    const [previousVersion, setPreviousVersion] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [displayVideosCount, setDisplayVideosCount] = useState(2);
    const [displayUseCasesCount, setDisplayUseCasesCount] = useState(3);
    const [displayBlogsCount, setDisplayBlogsCount] = useState(3);

    // Memoized getters
    const currentService = useMemo(() => {
        let result = null;
        if (tiles && tiles.length > 0 && tiles[0] && tiles[0].services) {
            tiles[0].services.forEach((service) => {
                if (service.serviceId === urlServiceId) {
                    if (service.serviceId !== selectedService.serviceId || currentTileId !== currentTileId) {
                        selectService(service, currentTileId);
                    }
                    result = service;
                }
            });
        }
        return result;
    }, [tiles, selectedService, currentTileId, urlServiceId, selectService]);

    const containsVersion = useMemo(
        () => currentService && 'apiVersions' in currentService && currentService.apiVersions,
        [currentService]
    );

    const basePath = useMemo(() => {
        let result = '';
        if (selectedService.basePath) {
            const version = selectedVersion || selectedService.defaultApiVersion;
            let gatewayUrl = '';
            if (selectedService.apis && selectedService.apis[version] && selectedService.apis[version].gatewayUrl) {
                gatewayUrl = selectedService.apis[version].gatewayUrl;
            }
            result = `/${selectedService.serviceId}/${gatewayUrl}`;
        }
        return result;
    }, [selectedService, selectedVersion]);

    const hasHomepage = useMemo(
        () =>
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0,
        [selectedService]
    );

    const apiVersions = useMemo(() => {
        if (!containsVersion) return [];
        return currentService.apiVersions.map((version) => {
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
                    onClick={() => setSelectedVersion(version)}
                    value={version}
                    style={tabStyle}
                    data-testid="version"
                >
                    {version}
                </MenuItem>
            );
        });
    }, [containsVersion, currentService, selectedVersion]);

    const handleDialogOpen = useCallback((service) => {
        if (selectedVersion === null) {
            setPreviousVersion(service.defaultApiVersion);
        } else {
            setPreviousVersion(selectedVersion);
        }
        setSelectedVersion('diff');
        setIsDialogOpen(true);
    }, [selectedVersion]);

    const handleDialogClose = useCallback(() => {
        setIsDialogOpen(false);
        setSelectedVersion(null);
    }, []);

    const showMoreVideos = useCallback(() => {
        setDisplayVideosCount((prev) => prev + videos.length);
    }, [videos.length]);

    const showMoreBlogs = useCallback(() => {
        setDisplayBlogsCount((prev) => prev + tutorials.length);
    }, [tutorials.length]);

    const showMoreUseCases = useCallback(() => {
        setDisplayUseCasesCount((prev) => prev + useCases.length);
    }, [useCases.length]);

    if (tiles === null || tiles === undefined || tiles.length === 0) {
        throw new Error('No tile is selected.');
    }

    const message = 'The API documentation was retrieved but could not be displayed.';
    const sso = selectedService.ssoAllInstances ? 'supported' : 'not supported';
    const apiPortalEnabled = isAPIPortal();
    const useCasesPresent = useCasesCounter !== 0;
    const videosPresent = videosCounter !== 0;
    const tutorialsPresent = tutorialsCounter !== 0;

    return (
        <>
            {currentService === null && (
                <Typography id="no-tiles-error" variant="h4">
                    <p>The service ID "{urlServiceId}" does not match any registered service</p>
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
                                    style={{ color: '#0056B3', marginLeft: '4px' }}
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
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                            {containsVersion && currentService && (
                                <Typography id="version-label" variant="subtitle2">
                                    Service ID and Version:
                                </Typography>
                            )}
                            {currentService && apiVersions?.length === 1 && apiVersions[0]?.key && (
                                <Typography id="single-api-version-label" variant="subtitle2">
                                    {apiVersions[0].key}
                                </Typography>
                            )}
                        </div>
                    </div>
                    {currentService && apiVersions?.length > 1 && (
                        <div id="version-div">
                            <Select
                                displayEmpty
                                id="version-menu"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                value={selectedVersion || currentService.defaultApiVersion}
                                data-testid="version-menu"
                                disableUnderline
                                onChange={(e) => setSelectedVersion(e.target.value)}
                            >
                                {apiVersions}
                            </Select>
                            <Button
                                id="compare-button"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                onClick={() => handleDialogOpen(currentService)}
                                key="diff"
                            >
                                <Typography className="version-text">Compare API Versions</Typography>
                            </Button>
                        </div>
                    )}
                    {selectedVersion !== 'diff' && <SwaggerContainer selectedVersion={selectedVersion} />}
                    {selectedVersion === 'diff' && isDialogOpen && containsVersion && (
                        <ServiceVersionDiffContainer
                            selectedVersion={previousVersion}
                            handleDialog={handleDialogClose}
                            serviceId={selectedService.serviceId}
                            versions={currentService.apiVersions}
                            isDialogOpen={isDialogOpen}
                        />
                    )}
                    {isAPIPortal() && (
                        <div id="detail-footer">
                            {useCasesPresent && (
                                <Typography
                                    className="footer-labels"
                                    id="use-cases-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    Use Cases ({useCasesCounter})
                                </Typography>
                            )}
                            <br />
                            <br />
                            <div id="blogs-container">
                                {useCases?.slice(0, displayUseCasesCount).map((useCase) => (
                                    <BlogContainer
                                        key={useCase.url}
                                        user={useCase.user}
                                        url={useCase.url}
                                        title={useCase.title}
                                    />
                                ))}
                            </div>
                            {useCasesCounter > displayUseCasesCount && displayUseCasesCount < useCases.length && (
                                <IconButton className="more-content-button" onClick={showMoreUseCases}>
                                    Show all ({useCasesCounter} articles)
                                </IconButton>
                            )}
                            <br />
                            <br />
                            {tutorialsPresent && (
                                <Typography
                                    className="footer-labels"
                                    id="tutorials-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    Getting Started ({tutorialsCounter})
                                </Typography>
                            )}
                            <br />
                            <div id="blogs-container">
                                {tutorials?.slice(0, displayBlogsCount).map((tutorial) => (
                                    <BlogContainer
                                        key={tutorial.url}
                                        user={tutorial.user}
                                        url={tutorial.url}
                                        title={tutorial.title}
                                    />
                                ))}
                            </div>
                            {tutorialsCounter > displayBlogsCount && displayBlogsCount < tutorials.length && (
                                <IconButton className="more-content-button" onClick={showMoreBlogs}>
                                    Show all ({tutorialsCounter} articles)
                                </IconButton>
                            )}
                            <br />
                            <br />
                            {videosPresent && (
                                <Typography
                                    className="footer-labels"
                                    id="videos-label"
                                    size="medium"
                                    variant="outlined"
                                >
                                    Videos ({videosCounter})
                                </Typography>
                            )}
                            <br />
                            <div>
                                {videos?.slice(0, displayVideosCount).map((url) => (
                                    <VideoWrapper key={url.url} url={url} />
                                ))}
                            </div>
                            {videosCounter > displayVideosCount && displayVideosCount < videos.length && (
                                <IconButton className="more-content-button" onClick={showMoreVideos}>
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

ServiceTab.propTypes = {
    videos: PropTypes.shape({
        length: PropTypes.func.isRequired,
        slice: PropTypes.func.isRequired,
    }).isRequired,
    tutorials: PropTypes.shape({
        length: PropTypes.func.isRequired,
        slice: PropTypes.func.isRequired,
    }).isRequired,
    useCases: PropTypes.shape({
        length: PropTypes.func.isRequired,
        slice: PropTypes.func.isRequired,
    }).isRequired,
    documentation: PropTypes.oneOfType([
        PropTypes.shape({
            label: PropTypes.string.isRequired,
            url: PropTypes.string.isRequired,
        }),
        PropTypes.arrayOf(
            PropTypes.shape({
                label: PropTypes.string.isRequired,
                url: PropTypes.string.isRequired,
            })
        ),
    ]).isRequired,
    selectedService: PropTypes.shape({
        title: PropTypes.string.isRequired,
    }).isRequired,
    selectService: PropTypes.func.isRequired,
    currentTileId: PropTypes.any,
    tiles: PropTypes.arrayOf(PropTypes.any),
    fetchTilesStop: PropTypes.func,
};
