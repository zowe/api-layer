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
    "baseUrl": "https://localhost:6000",
    "homePageUrl": "https://localhost:10010/",
    "basePath": "/service/api/v1",
    "apiDoc": null,
    "apiVersions": ["v1", "v2"],
    "defaultApiVersion": ["v1"]
}

const tiles = {
    "version": "1.0.0",
    "id": "apimediationlayer",
    "title": "API Mediation Layer API",
    "status": "UP",
    "description": "The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.",
    "services": [
        selectedService,
    ],

}

describe('>>> ServiceTab component tests', () => {
    it('should display service tab information', () => {

        const selectService = jest.fn();
        const serviceTab = shallow(<ServiceTab match={params} selectedService={selectedService} tiles={[tiles]} selectService={selectService}/>);
        serviceTab.setState({selectedVersion: "v1"})

        expect(serviceTab.find('Tooltip').exists()).toEqual(true);
        expect(serviceTab.find('Link').exists()).toEqual(true);
        expect(serviceTab.find('Link').first().props().href).toEqual("https://localhost:10010/");
        expect(serviceTab.find('Text').first().prop('children')).toEqual("API Gateway");
        expect(serviceTab.find('Text').at(1).prop('children')).toEqual(["Instance URL: ", "https://localhost:6000"]);
        expect(serviceTab.find('Text').at(2).prop('children')).toEqual(["API Base Path: ", "/service/api/v1"]);
        expect(serviceTab.find('Text').at(3).prop('children')).toEqual(["Service ID: ", "gateway"]);
        expect(serviceTab.find('Text').at(4).prop('children')).toEqual("API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.");
        expect(serviceTab.find('Text').at(5).prop('children')).toEqual('v1');
        expect(serviceTab.find('Text').at(6).prop('children')).toEqual('v2');
        expect(serviceTab.find('span').first().prop('style').background).toEqual('#d0d0d0');    //Check default api version is pre selected

    });

    it('should change selected version when clicking v2 api version', () => {
        const selectService = jest.fn();
        const serviceTab = shallow(<ServiceTab match={params} selectedService={selectedService} tiles={[tiles]} selectService={selectService}/>);
        serviceTab.setState({selectedVersion: "v1"})

        expect(serviceTab.find('span').first().prop('style').background).toEqual('#d0d0d0');
        expect(serviceTab.find('span').at(1).prop('style').background).toEqual(undefined);

        serviceTab.find('span').at(1).simulate('click')

        expect(serviceTab.find('span').at(1).prop('style').background).toEqual('#d0d0d0');
        expect(serviceTab.find('span').first().prop('style').background).toEqual(undefined);
        
    });

});
