import React, { Component } from 'react';
import { Text } from 'mineral-ui';
import SwaggerUI from '../Swagger/swagger';
import Shield from '../ErrorBoundary/Shield/Shield';

export default class ServiceTab extends Component {
    render() {
        const message = 'The API documentation was retrieved but could not be displayed!';
        const { match, tiles } = this.props;
        const serviceList = tiles.map(prop => prop.services);
        let wrongService = false;
        serviceList.forEach(serviceId => {
            serviceId.forEach(key => {
                wrongService = false;
                const previousServiceId = serviceId.map(prop => prop.serviceId)[0];
                if (key.serviceId !== match.params.serviceId && match.params.serviceId !== previousServiceId) {
                    wrongService = true;
                    return wrongService;
                }
            });
        });
        return (
            <React.Fragment>
                {wrongService && (
                    <Text element="h3" style={{ margin: '0 auto', 'background': '#ffff', width: '100%' }}>
                        <br/>
                        <br/>
                        <p style={{marginLeft: '122px'}}>The service ID with ID "{match.params.serviceId}" is not running and registered to the API
                        Mediation Layer</p>
                    </Text>
                )}
                <Shield title={message}>
                    <SwaggerUI serviceId={match.params.serviceId} version="v1" />
                </Shield>
            </React.Fragment>
        );
    }
}
