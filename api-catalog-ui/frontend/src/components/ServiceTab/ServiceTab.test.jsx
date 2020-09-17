import * as React from 'react';
import { shallow } from 'enzyme';
import ServiceTab from "./ServiceTab";

const params = {
    "path": "/tile/:tileID/:serviceId",
    "url": "/tile/apimediationlayer/gateway",
    "params": {
        "tileID": "apimediationlayer",
        "serviceId": "gateway"
    }
}
const selectedService = {
    "serviceId": "gateway",
    "title": "API Gateway",
    "description": "API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.",
    "status": "UP",
    "homePageUrl": "https://localhost:10010/",
    "basePath": "/service/api/v1",
    "apiDoc": null
}

const tiles = {
    "version": "1.0.0",
    "id": "apimediationlayer",
    "title": "API Mediation Layer API",
    "status": "UP",
    "description": "The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.",
    "services": [
        {
            "serviceId": "gateway",
            "title": "API Gateway",
            "description": "API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.",
            "homePageUrl": "https://localhost:10010/",
            "basePath": "/service/api/v1"
            "apiDoc": null
        },
    ],

}

describe('>>> ServiceTab component tests', () => {
    it('should display service tab informations', () => {

        const selectService = jest.fn();
        const serviceTab = shallow(<ServiceTab match={params} selectedService={selectedService} tiles={[tiles]} selectService={selectService}/>);

        expect(serviceTab.find('Tooltip').exists()).toEqual(true);
        expect(serviceTab.find('Link').exists()).toEqual(true);
        expect(serviceTab.find('Link').first().props().href).toEqual("https://localhost:10010/");
        expect(serviceTab.find('Text').first().prop('children')).toEqual("API Gateway");
        expect(serviceTab.find('Text').at(1).prop('children')).toEqual("[ API Base Path: /service/api/v1 ]")
        expect(serviceTab.find('Text').at(2).prop('children')).toEqual("API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.");

    });

});
