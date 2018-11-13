import React, { Component } from "react";
import { Button, Link, Text } from "mineral-ui";
import "../Swagger/swagger.css";
import "./ServiceTab.css";
import SwaggerUI from "../Swagger/swagger";
import Shield from "../ErrorBoundary/Shield/Shield";

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
                            <div style={{ margin: "0 auto", "background": "#ffff", width: "100%" }}>
                                <div style={{ margin: "10px 10px 10px 55px", "background": "#ffff", width: "100%" }}>
                                    <Text element="h2" fontWeight="bold">{selectedService.title}</Text>
                                    <div className='flexdiv'>
                                        <Text element="h4" fontWeight="bold" style={{margin: "0 0 0 10"}}>{selectedService.version}</Text>
                                        <Button minimal size="small" element="a" href={selectedService.homePageUrl} style={{margin: "0 0 0 10"}}>API Homepage</Button>
                                        <Text element="h4" fontWeight="bold" style={{margin: "0 0 0 10"}}>{selectedService.secured ? "secured" : "not secured"}</Text>
                                    </div>
                                    <Text>{selectedService.description}</Text>
                                </div>
                            </div>
                            < SwaggerUI serviceId={match.params.serviceId} version="v1"/>
                        </React.Fragment>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
