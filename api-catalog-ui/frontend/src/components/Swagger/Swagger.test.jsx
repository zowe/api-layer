/* eslint-disable no-undef */
import { shallow } from 'enzyme';
import SwaggerUI from './Swagger';
import { act } from "react-dom/test-utils";
import { render } from 'react-dom';

describe('>>> Swagger component tests', () => {
    it('should not render swagger if apiDoc is null', () => {
        const service = {
            serviceId: 'testservice',
            title: 'Spring Boot Enabler Service',
            description: 'Dummy Service for enabling others',
            status: 'UP',
            secured: false,
            homePageUrl: 'http://localhost:10013/enabler/',
            basePath: '/enabler/api/v1',
            apiDoc: null,
        };
        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service} />
            </div>
        );
        const swaggerDiv = wrapper.find('#swaggerContainer');

        expect(swaggerDiv.length).toEqual(0);
    });

    it('should transform swagger server url', async () => {
        const endpoint = '/enabler/api/v1';
        const service = {
            serviceId: 'testservice',
            title: 'Spring Boot Enabler Service',
            description: 'Dummy Service for enabling others',
            status: 'UP',
            secured: false,
            homePageUrl: 'http://localhost:10013/enabler/',
            basePath: '/enabler/api/v1',
            apiDoc: JSON.stringify({
                openapi: "3.0.0",
                servers: [{url: 'https://bad.com' + endpoint}],
            }),
            apiId: {
                default: 'enabler'
            }
        };

        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () => render(<SwaggerUI selectedService={service}/>, container));
        expect(container.textContent).toContain('Servershttp://localhost' + endpoint);
    });
});
