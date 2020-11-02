import { Link, Text, Tooltip } from 'mineral-ui';
import React, { Component } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import '../Swagger/Swagger.css';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import './ServiceTab.css';

export default class ServiceTab extends Component {
    constructor(props) {
        super(props);
        this.state = {
            currentService: null,
            invalidService: true,
            selectedVersion: null,
        }
    }

    componentDidMount() {
        const {
            match: {
                params: { tileID, serviceId },
            },
            tiles,
            selectService,
            selectedService,
            selectedTile,
        } = this.props;
        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error('No tile is selected.');
        }
        tiles[0].services.forEach(service => {
            if (service.serviceId === serviceId) {
                const currentService = service;
                if (currentService.serviceId !== selectedService.serviceId || selectedTile !== tileID) {
                    selectService(currentService, tileID);
                }
                this.setState({"invalidService": false});
                this.setState({currentService});
            }
        });
    }

    componentDidUpdate(){
        console.log(this.props);
    }

    render() {
        const { serviceId, selectedService } = this.props;
        const { invalidService, currentService, selectedVersion } = this.state;
        const message = 'The API documentation was retrieved but could not be displayed.';        
        const hasHomepage =
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0;
        
        let apiVersions = [];
        if(currentService && currentService.apiVersions) {
            apiVersions = currentService.apiVersions.map(version => {
                //TODO:: when setting state need to extrapolate value from version being clicked on and map to v1, v2, etc.
                //TODO:: Fix styling of componetn, currently just text
                return <span key={version} onClick={()=>{this.setState({selectedVersion: 'v1'})}}>{version}</span>
            });
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
                                {apiVersions}
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
                            </div>
                            <SwaggerContainer/>
                        </React.Fragment>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
