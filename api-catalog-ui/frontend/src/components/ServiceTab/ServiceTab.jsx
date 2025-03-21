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
import { useState } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';
import GraphQLUIApiml from "../GraphQL/GraphQLUIApiml";

function ServiceTab({service}) {

    const  containsVersion = () => {
        return service && 'apiVersions' in service && service.apiVersions;
    }

    const [selectedVersion, setSelectedVersion] = useState(null);
    const [previousVersion, setPreviousVersion] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const basePath = () => {
        if (!service?.basePath) {
            if (service?.instances?.[0]?.includes('gateway')) {
                // Return the basePath right away, since it's a GW instance (either primary or additional)
                return service.basePath;
            } else {
                const version = selectedVersion || service.defaultApiVersion;
                let gatewayUrl = '';
                if (service.apis && service.apis[version]) {
                    gatewayUrl = service.apis[version].gatewayUrl;
                }
                // Take the first part of the basePath and then add the gatewayUrl
                return `/${service.serviceId}/${gatewayUrl}`;
            }
        }
        return service.basePath;
    }

    const hasHomepage = () => {
        return (
            service.homePageUrl !== null &&
            service.homePageUrl !== undefined &&
            service.homePageUrl.length > 0
        );
    }

    const apiVersions = () => {
        let apiVersions = [];
        if (containsVersion()) {
            apiVersions = service.apiVersions.map((version) => {
                // Pre select default version or if only one version exists select that
                let tabStyle = {};
                if (
                    selectedVersion === null &&
                    (service.defaultApiVersion === version || service.apiVersions.length === 1)
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

    const graphqlUrl = getGraphqlUrl(service.apis);
    const title = graphqlUrl ? 'GraphQL' : 'Swagger';
    const showVersionDiv = !graphqlUrl;
    const message = 'The API documentation was retrieved but could not be displayed.';
    const sso = service.ssoAllInstances ? 'supported' : 'not supported';

    return (
        <>

            <Shield title={message}>
                <div className="serviceTab">
                    <div className="header">
                        <Typography id="service-title" data-testid="service" variant="h4">
                            {service.title}
                        </Typography>
                        {hasHomepage() && (
                            <>
                                {service.status === 'UP' && (
                                    <Tooltip
                                        data-testid="tooltip"
                                        key={service.serviceId}
                                        title="Open Service Homepage"
                                        placement="bottom"
                                    >
                                        <Link data-testid="link" href={service.homePageUrl}>
                                            <strong>Service Homepage</strong>
                                        </Link>
                                    </Tooltip>
                                )}
                                {service.status === 'DOWN' && (
                                    <Tooltip
                                        key={service.serviceId}
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
                                key={service.serviceId}
                                title="The identifier for this service"
                                placement="bottom"
                            >
                                <Typography data-testid="service-id" variant="subtitle2">
                                    <label htmlFor="serviceId">Service ID:</label>
                                    <span id="serviceId">{service.serviceId}</span>
                                </Typography>
                            </Tooltip>
                            <Tooltip
                                key={service.ssoAllInstances}
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
                            {service.description}
                        </Typography>
                        <br />
                        <Typography id="swagger-label" className="title1" size="medium" variant="outlined">
                            {title}
                        </Typography>
                        {showVersionDiv && (
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                {containsVersion() && service && (
                                    <Typography id="version-label" variant="subtitle2">
                                        Service ID and Version:
                                    </Typography>
                                )}
                                {service && av?.length === 1 && av[0]?.key && (
                                    <Typography id="single-api-version-label" variant="subtitle2">
                                        {av[0].key}
                                    </Typography>
                                )}
                            </div>
                        )}
                    </div>
                    {showVersionDiv && service && av?.length > 1 && (
                        <div id="version-div">
                            <Select
                                displayEmpty
                                id="version-menu"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                value={
                                    selectedVersion
                                        ? selectedVersion
                                        : service.defaultApiVersion
                                }
                                data-testid="version-menu"
                                disableUnderline
                            >
                                {av}
                            </Select>
                            <Button
                                id="compare-button"
                                data-testid="diff-button"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                onClick={() => handleDialogOpen(service)}
                                key="diff"
                            >
                                <Typography className="version-text">Compare API Versions</Typography>
                            </Button>
                        </div>
                    )}
                    {graphqlUrl !== null && <GraphQLUIApiml graphqlUrl={graphqlUrl} />}
                    {graphqlUrl === null && selectedVersion !== 'diff' && (
                        <SwaggerContainer selectedVersion={selectedVersion} />
                    )}
                    {graphqlUrl === null && selectedVersion === 'diff' && isDialogOpen && containsVersion() && (
                        <ServiceVersionDiffContainer
                            selectedVersion={previousVersion}
                            handleDialog={handleDialogClose}
                            serviceId={service.serviceId}
                            versions={service.apiVersions}
                            isDialogOpen={isDialogOpen}
                        />
                    )}
                </div>
            </Shield>
        </>
    );

}

export default ServiceTab;

