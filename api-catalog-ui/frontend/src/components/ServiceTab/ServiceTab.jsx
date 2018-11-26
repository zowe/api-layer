import { Link, Text, Tooltip } from "mineral-ui";
import React, { Component } from "react";
import Shield from "../ErrorBoundary/Shield/Shield";
import "../Swagger/Swagger.css";
import SwaggerContainer from "../Swagger/SwaggerContainer";
import "./ServiceTab.css";

export default class ServiceTab extends Component {
    render() {
        const message = "The API documentation was retrieved but could not be displayed.";
        const {
            match: {
                params: { tileID, serviceId }
            },
            tiles, selectService, selectedService, selectedTile
        } = this.props;
        let currentService = null;
        let invalidService = true;
        const hasHomepage = selectedService.homePageUrl !== null && selectedService.homePageUrl !== undefined && selectedService.homePageUrl.length > 0;

        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error("No tile is selected.");
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
                    <Text element="h3" style={{ margin: "0 auto", "background": "#ffff", width: "100%" }}>
                        <br/>
                        <br/>
                        <p style={{ marginLeft: "122px" }}>This tile does not contain service "{serviceId}"</p>
                    </Text>
                )}
                <Shield title={message}>
                    {selectedService !== null && (
                        <React.Fragment>
                            <div style={{ "background": "#ffff" }}>
                                <div style={{ margin: "20px 0px 0px 55px", "background": "#ffff", width: "100%" }}>
                                    <Text element="h2" color="#3b4151" fontWeight="bold">{selectedService.title}</Text>
                                    {hasHomepage && (
                                        <React.Fragment>
                                            {
                                                selectedService.status === "UP" && (
                                                    <Tooltip key={selectedService.serviceId} content="Open API Homepage"
                                                             placement="bottom">
                                                        <Link href={selectedService.homePageUrl}><strong>API
                                                            Homepage</strong></Link>
                                                    </Tooltip>
                                                )
                                            }
                                            {selectedService.status === "DOWN" && (
                                                <Tooltip key={selectedService.serviceId}
                                                         content="API Homepage navigation is disabled as the service is not running"
                                                         placement="bottom">
                                                    <Link variant="danger"><strong>API Homepage</strong></Link>
                                                </Tooltip>
                                            )}
                                        </React.Fragment>
                                    )}
                                    <Text style={{ marginTop: "15px" }}>{selectedService.description}</Text>
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
