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
import {Component, useState} from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import GraphQLContainer from '../GraphQL/GraphQLUIApimlContainer';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';
import {useParams} from "react-router";

// const { selectedVersion, isDialogOpen } = this.state;

function ServiceTab({tiles,selectedService,basePath,selectService,currentTileId,selectedTile}) {
    // constructor(props) {
    //     super(props);
    //     this.state = {
    //         selectedVersion: null,
    //         previousVersion: null,
    //         isDialogOpen: false,
    //     };
    //     this.handleDialogClose = this.handleDialogClose.bind(this);
    // }

    const  containsVersion = () =>{
        return currentService && 'apiVersions' in currentService && currentService.apiVersions;
    }

    const [selectedVersion, setSelectedVersion] = useState(null);
    const [previousVersion, setPreviousVersion] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const basePat = () => {
        // const { selectedService } = this.props;


        let basePath = '';
        if (selectedService?.basePath) {
            if (selectedService?.instances?.[0]?.includes('gateway')) {
                // Return the basePath right away, since it's a GW instance (either primary or additional)
                basePath = selectedService.basePath;
            } else {
                const version = selectedVersion || selectedService.defaultApiVersion;
                let gatewayUrl = '';
                if (selectedService.apis && selectedService.apis[version] && selectedService.apis[version].gatewayUrl) {
                    gatewayUrl = selectedService.apis[version].gatewayUrl;
                }
                // Take the first part of the basePath and then add the gatewayUrl
                basePath = `/${selectedService.serviceId}/${gatewayUrl}`;
            }
        }
        return basePath;
    }

    const currentService = () => {
        let currentService = null;

        // const {
        //     // match: {
        //     //     params: { serviceId },
        //     // },
        //     selectedService,
        //     selectedTile,
        //     selectService,
        //     currentTileId,
        //     tiles,
        // } = this.props;
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

    const hasHomepage = () => {
        // const { selectedService } = this.props;
        return (
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0
        );
    }

    const apiVersions = () => {
        let apiVersions = [];


        const { currentService } = this;

        if (containsVersion()) {
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
                            setSelectedVersion(version)
                            // this.setState({ selectedVersion: version });
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

    const handleDialogOpen = (currentService) => {
        const { selectedVersion } = this.state;
        if (selectedVersion === null) {
            setPreviousVersion(currentService.defaultApiVersion)
            // this.setState({ previousVersion: currentService.defaultApiVersion });
        } else {
            setPreviousVersion(selectedVersion)
            // this.setState({ previousVersion: selectedVersion });
        }
        setSelectedVersion('diff');
        setIsDialogOpen(true);
        setPreviousVersion(selectedVersion ?? currentService.defaultApiVersion);
        // this.setState({
        //     isDialogOpen: true,
        //     selectedVersion: 'diff',
        //     previousVersion: selectedVersion ?? currentService.defaultApiVersion,
        // });
    };

    const handleDialogClose = () => {
        setIsDialogOpen(false);
        setSelectedVersion(null);
        // this.setState({ isDialogOpen: false, selectedVersion: null });
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
    console.log('tttail',tiles)

    const graphqlUrl = getGraphqlUrl(selectedService.apis);
    const title = graphqlUrl ? 'GraphQL' : 'Swagger';
    const showVersionDiv = !graphqlUrl;
    const message = 'The API documentation was retrieved but could not be displayed.';
    const sso = selectedService.ssoAllInstances ? 'supported' : 'not supported';
    const { serviceId } = useParams();
    console.log('serrID',serviceId)
    return (
        <>
            {currentService() === null && (
                <Typography id="no-tiles-error" variant="h4">
                    <p>The service ID "{serviceId}" does not match any registered service</p>
                </Typography>
            )}
            <Shield title={message}>
                <div className="serviceTab">
                    <div className="header">
                        <Typography id="service-title" data-testid="service" variant="h4">
                            {selectedService.title}
                        </Typography>
                        {hasHomepage && (
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
                                key={basePath}
                                title="The path used by the Gateway to access API endpoints. This can be used to identify a service in client tools like Zowe CLI and Zowe explorer."
                                placement="bottom"
                            >
                                <Typography data-testid="base-path" variant="subtitle2">
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
                        )}
                    </div>
                    {showVersionDiv && currentService && apiVersions?.length > 1 && (
                        <div id="version-div">
                            <Select
                                displayEmpty
                                id="version-menu"
                                style={{ backgroundColor: '#fff', color: '#0056B3' }}
                                value={
                                    selectedVersion
                                        ? selectedVersion
                                        : currentService.defaultApiVersion
                                }
                                data-testid="version-menu"
                                disableUnderline
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
                    {graphqlUrl !== null && <GraphQLContainer graphqlUrl={graphqlUrl} />}
                    {graphqlUrl === null && selectedVersion !== 'diff' && (
                        <SwaggerContainer selectedVersion={selectedVersion} />
                    )}
                    {graphqlUrl === null && selectedVersion === 'diff' && isDialogOpen && containsVersion && (
                        <ServiceVersionDiffContainer
                            selectedVersion={previousVersion}
                            handleDialog={handleDialogClose}
                            serviceId={selectedService.serviceId}
                            versions={currentService.apiVersions}
                            isDialogOpen={isDialogOpen}
                        />
                    )}
                </div>
            </Shield>
        </>
    );

}

export default ServiceTab;

// ServiceTab.propTypes = {
//     selectedService: PropTypes.shape({
//         title: PropTypes.string,
//         description: PropTypes.string,
//         basePath: PropTypes.string,
//         homePageUrl: PropTypes.string,
//         defaultApiVersion: PropTypes.string,
//         apis: PropTypes.objectOf(
//             PropTypes.shape({
//                 gatewayUrl: PropTypes.string,
//             })
//         ),
//         instances: PropTypes.arrayOf(PropTypes.string),
//         apiVersions: PropTypes.arrayOf(PropTypes.string),
//         serviceId: PropTypes.string,
//         status: PropTypes.string,
//         ssoAllInstances: PropTypes.string,
//     }).isRequired,
// };
