import { Link, Text, Tooltip } from 'mineral-ui';
import React, { Component } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import '../Swagger/Swagger.css';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import './ServiceTab.css';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';

export default class ServiceTab extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedVersion: null,
        }
    }

    render() {
        const { 
            match : {
                params: {tileID, serviceId},
            },
            tiles, 
            selectedService,
            selectedTile,
            selectService,
        } = this.props;
        const { selectedVersion } = this.state;
        let currentService = null;	
        let invalidService = true;
        tiles[0].services.forEach(service => {
            if (service.serviceId === serviceId) {
                
                if (service.serviceId !== selectedService.serviceId || selectedTile !== tileID) {
                    selectService(service, tileID);
                }
                invalidService = false;
                currentService = service;
            }
        });
        const message = 'The API documentation was retrieved but could not be displayed.';        
        const hasHomepage =
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0;
        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error('No tile is selected.');
        }
        let apiVersions = [];
        if(currentService && currentService.apiVersions) {
            apiVersions = currentService.apiVersions.map(version => {
                //Pre select default version or if only one version exists select that
                let selectedStyle = {};
                if(selectedVersion === null && (currentService.defaultApiVersion === version || currentService.apiVersions.length === 1)){
                    selectedStyle = { backgroundColor: '#fff' };
                }
                if (selectedVersion === version) {
                    selectedStyle = { backgroundColor: '#fff' };
                }
                return <span 
                    className="nav-tab"
                    key={version} 
                    style={selectedStyle}
                    onClick={ () => { this.setState({selectedVersion: version}); }}>
                        <Text>{version}</Text>
                    </span>
            });
            if(apiVersions.length >= 2){
                apiVersions.push(
                    <span 
                        className="nav-tab" 
                        onClick={ () => { this.setState({selectedVersion: 'diff'})}}
                        style={selectedVersion === 'diff' ? { backgroundColor: '#fff'} : {} }
                        key="diff"
                    >
                        <Text>Compare</Text>
                    </span>
                );
            }
        }

        return (
            <React.Fragment>
                {invalidService && (
                    <Text element="h3" style={{ margin: '0 auto', background: '#ffff', width: '100vh' }}>
                        <br />
                        <br />
                        <p style={{ marginLeft: '122px' }}>This tile does not contain service "{serviceId}"</p>
                    </Text>
                )}
                <Shield title={message}>
                    {selectedService !== null && (
                        <React.Fragment>
                            <div style={{ background: '#ffff' }}>
                                <div style={{ margin: '20px 0px 0px 55px', background: '#ffff', width: '100vh' }}>
                                    <Text element="h2" color="#3b4151" fontWeight="bold">
                                        {selectedService.title}
                                    </Text>
                                    {hasHomepage && (
                                        <React.Fragment>
                                            {selectedService.status === 'UP' && (
                                                <Tooltip
                                                    key={selectedService.serviceId}
                                                    content="Open Service Homepage"
                                                    placement="bottom"
                                                >
                                                    <Link href={selectedService.homePageUrl}>
                                                        <strong>Service Homepage</strong>
                                                    </Link>
                                                </Tooltip>
                                            )}
                                            {selectedService.status === 'DOWN' && (
                                                <Tooltip
                                                    key={selectedService.serviceId}
                                                    content="API Homepage navigation is disabled as the service is not running"
                                                    placement="bottom"
                                                >
                                                    <Link variant="danger">
                                                        <strong>API Homepage</strong>
                                                    </Link>
                                                </Tooltip>
                                            )}
                                        </React.Fragment>
                                    )}
                                    <br />
                                    <br />
                                    <Tooltip
                                        key={selectedService.baseUrl}
                                        content="The instance URL for this service"
                                        placement="bottom"
                                    >
                                        <Text style={{ fontSize: '13px' }}>Instance URL: {selectedService.baseUrl}</Text>
                                    </Tooltip>
                                    <br/>
                                    <Tooltip
                                        key={selectedService.basePath}
                                        content="The path used by the Gateway to access API endpoints. This can be used to identify a service in client tools like Zowe CLI and Zowe explorer."
                                        placement="bottom"
                                    >
                                        <Text style={{ fontSize: '13px' }}>API Base Path: {selectedService.basePath}</Text>
                                    </Tooltip>
                                    <br/>
                                    <Tooltip
                                        key={selectedService.serviceId}
                                        content="The identifier for this service"
                                        placement="bottom"
                                    >
                                        <Text style={{ fontSize: '13px' }}>Service ID: {selectedService.serviceId}</Text>
                                    </Tooltip>
                                    <Text style={{ marginTop: '15px' }}>{selectedService.description}</Text>
                                </div>
                                <div className="tabs-container" style={{width: '100%'}}>
                                    {apiVersions}
                                </div>
                            {selectedVersion !== 'diff' ? 
                                <SwaggerContainer selectedVersion={selectedVersion} /> :
                                <ServiceVersionDiffContainer serviceId={selectedService.serviceId} versions={currentService.apiVersions} />}
                            </div>
                        </React.Fragment>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
