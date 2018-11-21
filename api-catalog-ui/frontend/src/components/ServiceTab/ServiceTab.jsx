import { Link, Text } from 'mineral-ui';
import '../Swagger/Swagger.css';
import './ServiceTab.css';
import React, { Component } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import SwaggerContainer from '../Swagger/SwaggerContainer';

export default class ServiceTab extends Component {
    render() {
        const message = 'The API documentation was retrieved but could not be displayed.';
        const {
            match: {
                params: { tileID, serviceId },
            },
            tiles, selectService, selectedService, selectedTile } = this.props;
        let currentService = null;
        let invalidService = true;

        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error("No tile is selected.")
        }
        tiles[0].services.forEach(service => {
            if (service.serviceId === serviceId) {
                currentService = service;
                if (currentService.serviceId !== selectedService.serviceId || selectedTile !== tileID) {
                    selectService(currentService, tileID);
                }
                invalidService = false;
            }
        });
        return (
            <React.Fragment>
                {invalidService && (
                    <Text element="h3" style={{ margin: '0 auto', 'background': '#ffff', width: '100%' }}>
                        <p style={{ marginLeft: '55px', marginTop: '50px' }}>This tile does not contain service
                            "{serviceId}"</p>
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
                            <SwaggerContainer />
                        </React.Fragment>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
