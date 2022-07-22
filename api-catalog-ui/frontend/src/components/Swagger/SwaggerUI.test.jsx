/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { act } from 'react-dom/test-utils';
import { render } from 'react-dom';
import { shallow } from 'enzyme';
import SwaggerUI from './SwaggerUI';

describe('>>> Swagger component tests', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

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

    it('should not render swagger if apiDoc is undefined', async () => {
        const service = {
            serviceId: 'testservice',
            title: 'Spring Boot Enabler Service',
            description: 'Dummy Service for enabling others',
            status: 'UP',
            secured: false,
            homePageUrl: 'http://localhost:10013/enabler/',
            basePath: '/enabler/api/v1',
        };

        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () => render(<SwaggerUI selectedService={service} />, container));
        expect(container.textContent).toContain(`API documentation could not be retrieved`);
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
                openapi: '3.0.0',
                servers: [{ url: `https://bad.com${endpoint}` }],
            }),
            apis: {
                default: { apiId: 'enabler' },
            },
        };

        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () => render(<SwaggerUI selectedService={service} />, container));
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint}`);
    });

    it('should update swagger', async () => {
        const endpoint1 = '/oldenabler/api/v1';
        const endpoint2 = '/newenabler/api/v2';
        const service1 = {
            serviceId: 'oldservice',
            title: 'Spring Boot Enabler Service',
            description: 'Dummy Service for enabling others',
            status: 'UP',
            secured: false,
            homePageUrl: 'http://localhost:10013/oldenabler/',
            basePath: '/oldenabler/api/v1',
            apiDoc: JSON.stringify({
                openapi: '3.0.0',
                servers: [{ url: `https://bad.com${endpoint1}` }],
            }),
            apis: {
                default: { apiId: 'oldenabler' },
            },
        };

        const service2 = {
            serviceId: 'newservice',
            title: 'Spring Boot Enabler Service',
            description: 'Dummy Service for enabling others',
            status: 'UP',
            secured: false,
            homePageUrl: 'http://localhost:10013/newenabler/',
            basePath: '/newenabler/api/v1',
            apiDoc: JSON.stringify({
                openapi: '3.0.0',
                servers: [{ url: `https://bad.com${endpoint2}` }],
            }),
            apis: {
                default: { apiId: 'oldenabler' },
            },
        };

        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () => render(<SwaggerUI selectedService={service1} />, container));
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint1}`);
        await act(async () => render(<SwaggerUI selectedService={service2} />, container));
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint2}`);
    });
});
