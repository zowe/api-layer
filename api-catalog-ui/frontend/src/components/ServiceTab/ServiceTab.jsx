import React, { Component } from 'react';
import { Text } from 'mineral-ui';
import SwaggerUI from '../Swagger/swagger';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class ServiceTab extends Component {
    render() {
        const message = 'The API documentation was retrieved but could not be displayed.';
        const { match, tiles } = this.props;
        let selectedService = null;
        let invalidService = true;

        tiles[0].services.forEach(service => {
            if (service.serviceId === match.params.serviceId) {
                selectedService = service;
                invalidService = false;
            }
        });
        return (
            <React.Fragment>
                {invalidService && (
                    <Text element="h3" style={{ margin: '0 auto', 'background': '#ffff', width: '100%' }}>
                        <p style={{marginLeft: '55px', marginTop: '50px'}}>This tile does not contain service "{match.params.serviceId}"</p>
                    </Text>
                )}
                <Shield title={message}>
                    {selectedService !== null && (
                        <SwaggerUI service={selectedService}/>
                    )}
                </Shield>
            </React.Fragment>
        );
    }
}
