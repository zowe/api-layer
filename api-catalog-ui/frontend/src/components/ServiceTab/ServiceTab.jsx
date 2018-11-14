import React, { Component } from 'react';
import { Link, Text } from 'mineral-ui';
import '../Swagger/swagger.css';
import './ServiceTab.css';
import SwaggerUI from '../Swagger/swagger';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class ServiceTab extends Component {
    render() {
        const message = "The API documentation was retrieved but could not be displayed!";
        const { match, tiles } = this.props;
        const serviceList = tiles.map(prop => prop.services);
        var selectedService = null;
        let wrongService = false;
        serviceList.forEach(serviceId => {
            serviceId.forEach(service => {
                wrongService = false;
                const previousServiceId = serviceId.map(prop => prop.serviceId)[0];
                if (service.serviceId !== match.params.serviceId && match.params.serviceId !== previousServiceId) {
                    wrongService = true;
                    return wrongService;
                }
                if (service.serviceId === match.params.serviceId) {
                    selectedService = service;
                }
            });
        });
        return (
            <React.Fragment>
                {wrongService && (
                    <Text element="h3" style={{ margin: "0 auto", "background": "#ffff", width: "100%" }}>
                        <br/>
                        <br/>
                        <p style={{ marginLeft: "122px" }}>The service ID with ID "{match.params.serviceId}" is not
                            running and registered to the API
                            Mediation Layer</p>
                    </Text>
                )}
                <Shield title={message}>
                    {selectedService !== null && (
                        <React.Fragment>
                            <div style={{"background": "#ffff" }}>
                                <div style={{ margin: "20px 0px 0px 55px", "background": "#ffff", width: "100%" }}>
                                    <Text element="h2" color="#3b4151" fontWeight="bold">{selectedService.title}</Text>
                                    <Link href={selectedService.homePageUrl}><strong>API Homepage</strong></Link>
                                    <Text style={{marginTop: "15px"}}>{selectedService.description}</Text>
                                </div>
                            </div>
                            <SwaggerUI serviceId={match.params.serviceId} version="v1" />
                        </React.Fragment>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
