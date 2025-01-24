/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Button, Link, MenuItem, Select, Tooltip, Typography } from '@material-ui/core';
import { useState} from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import GraphQLContainer from '../GraphQL/GraphQLUIApimlContainer';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';

function ServiceTab({tiles,selectedService,selectService,currentTileId,selectedTile}) {

    const  containsVersion = () =>{
        return selectedService && 'apiVersions' in selectedService && selectedService.apiVersions;
    }

    const [selectedVersion, setSelectedVersion] = useState(null);
    const [previousVersion, setPreviousVersion] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const basePath = () => {
        if (!selectedService?.basePath) {
            if (selectedService?.instances?.[0]?.includes('gateway')) {
                // Return the basePath right away, since it's a GW instance (either primary or additional)
                return selectedService.basePath;
            } else {
                const version = selectedVersion || selectedService.defaultApiVersion;
                let gatewayUrl = '';
                if (selectedService.apis && selectedService.apis[version]) {
                    gatewayUrl = selectedService.apis[version].gatewayUrl;
                }
                // Take the first part of the basePath and then add the gatewayUrl
                return `/${selectedService.serviceId}/${gatewayUrl}`;
            }
        }
        return selectedService.basePath;
    }

    const setCurrentService = () => {
        let currentService = null;

        if (tiles && tiles.length > 0 && tiles[0] && tiles[0].services) {
            tiles[0].services.forEach((service) => {
                if (service.serviceId === serviceId) {
                    if (service.serviceId !== selectedService.serviceId || selectedTile !== currentTileId) {
                        console.log(service)
                        console.log(currentTileId)
                        selectService(service, currentTileId);
                    }
                    currentService = service;
                }
            });
        }
        return currentService;
    }

    const hasHomepage = () => {
        return (
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0
        );
    }

    const apiVersions = () => {
        let apiVersions = [];

        if (containsVersion()) {
            apiVersions = selectedService.apiVersions.map((version) => {
                // Pre select default version or if only one version exists select that
                let tabStyle = {};
                if (
                    selectedVersion === null &&
                    (selectedService.defaultApiVersion === version || selectedService.apiVersions.length === 1)
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
                            setSelectedVersion(version)
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

    const av = apiVersions();

    const handleDialogOpen = (currentService) => {

        if (selectedVersion === null) {
            setPreviousVersion(currentService.defaultApiVersion)
        } else {
            setPreviousVersion(selectedVersion)
        }
        setSelectedVersion('diff');
        setIsDialogOpen(true);
        setPreviousVersion(selectedVersion ?? currentService.defaultApiVersion);
    };

    const handleDialogClose = () => {
        setIsDialogOpen(false);
        setSelectedVersion(null);
    };

    const getGraphqlUrl = (apis) => {
        if (!apis || typeof apis !== 'object') {
            return null;
        }
        const apiKey = Object.keys(apis).find((key) => apis[key]?.graphqlUrl);
        return apiKey ? apis[apiKey].graphqlUrl : null;
    };

    if (tiles === null || tiles === undefined || tiles.length === 0) {
        throw new Error('No tile is selected.');
    }

    const graphqlUrl = getGraphqlUrl(selectedService.apis);
    const title = graphqlUrl ? 'GraphQL' : 'Swagger';
    const showVersionDiv = !graphqlUrl;
    const message = 'The API documentation was retrieved but could not be displayed.';
    const sso = selectedService.ssoAllInstances ? 'supported' : 'not supported';

    return (
        <>

            <Shield title={message}>
                <div className="serviceTab">
                    <div className="header">
                        <Typography id="service-title" data-testid="service" variant="h4">
                            {selectedService.title}
                        </Typography>
                        {hasHomepage() && (
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
                        <div className="apiInfo-item">
                            <Tooltip
                                key={basePath()}
                                title="The path used by the Gateway to access API endpoints. This can be used to identify a service in client tools like Zowe CLI and Zowe explorer."
                                placement="bottom"
                            >
                                <Typography data-testid="base-path" variant="subtitle2">
                                    <label htmlFor="apiBasePath">API Base Path:</label>
                                    <span id="apiBasePath">{basePath()}</span>
                                </Typography>
                            </Tooltip>
                            <Tooltip
                                key={selectedService.serviceId}
                                title="The identifier for this service"
                                placement="bottom"
                            >
                                <Typography data-testid="service-id" variant="subtitle2">
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
                                    <label htmlFor="sso">SSO:</label>
                                    <span id="sso">{sso}</span>
                                </Typography>
                            </Tooltip>
                        </div>

                        <Typography data-testid="description" variant="subtitle2" style={{ color: 'black' }}>
                            {selectedService.description}
                        </Typography>
                        <br />
                        <Typography id="swagger-label" className="title1" size="medium" variant="outlined">
                            {title}
                        </Typography>
                        {showVersionDiv && (
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                {containsVersion() && selectedService && (
                                    <Typography id="version-label" variant="subtitle2">
                                        Service ID and Version:
                                    </Typography>
                                )}
                                {selectedService && av?.length === 1 && av[0]?.key && (
                                    <Typography id="single-api-version-label" variant="subtitle2">
                                        {av[0].key}
                                    </Typography>
                                )}
                            </div>
                        )}
                    </div>
                    {showVersionDiv && selectedService && av?.length > 1 && (
                        <div id="version-div">
                            <Select
                                displayEmpty
                                id="version-menu"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                value={
                                    selectedVersion
                                        ? selectedVersion
                                        : selectedService.defaultApiVersion
                                }
                                data-testid="version-menu"
                                disableUnderline
                            >
                                {av}
                            </Select>
                            <Button
                                id="compare-button"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                onClick={() => handleDialogOpen(selectedService)}
                                key="diff"
                            >
                                <Typography className="version-text">Compare API Versions</Typography>
                            </Button>
                        </div>
                    )}
                    {graphqlUrl !== null && <GraphQLContainer graphqlUrl={graphqlUrl} />}
                    {graphqlUrl === null && selectedVersion !== 'diff' && (
                        <SwaggerContainer selectedVersion={selectedVersion} />
                    )}
                    {graphqlUrl === null && selectedVersion === 'diff' && isDialogOpen && containsVersion() && (
                        <ServiceVersionDiffContainer
                            selectedVersion={previousVersion}
                            handleDialog={handleDialogClose}
                            serviceId={selectedService.serviceId}
                            versions={selectedService.apiVersions}
                            isDialogOpen={isDialogOpen}
                        />
                    )}
                </div>
            </Shield>
        </>
    );

}

export default ServiceTab;

