/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { act } from 'react';
import { createRoot } from 'react-dom/client';
import { shallow } from 'enzyme';
import { describe, expect, it, jest } from '@jest/globals';
import SwaggerUI from './SwaggerUIApiml';

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
        service.apis = {
            codeSnippet: {
                codeBlock: 'code',
                endpoint: '/test',
                language: 'java',
            },
        };
        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service} />
            </div>
        );
        const swaggerDiv = wrapper.find('#swaggerContainer');

        expect(swaggerDiv.length).toEqual(0);
    });

    it('should not render swagger if apis default is provided', () => {
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
            }),
            apis: {
                default: {
                    apiId: 'enabler',
                    codeSnippet: {
                        codeBlock: 'code',
                        endpoint: '/test',
                        language: 'java',
                    },
                },
            },
            defaultApiVersion: 0,
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
            defaultApiVersion: 0,
        };
        service.apis = [
            {
                default: { apiId: 'enabler' },
                codeSnippet: {
                    codeBlock: 'code',
                    endpoint: '/test',
                    language: 'java',
                },
            },
        ];

        const container = document.createElement('div');
        document.body.appendChild(container);
        await act(async () => createRoot(container).render(<SwaggerUI selectedService={service} />, container));
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
                default: {
                    apiId: 'enabler',
                    codeSnippet: {
                        codeBlock: 'code',
                        endpoint: '/test',
                        language: 'java',
                    },
                },
            },
            defaultApiVersion: 0,
        };
        service.apis = [
            {
                default: { apiId: 'enabler' },
                codeSnippet: {
                    codeBlock: 'code',
                    endpoint: '/test',
                    language: 'java',
                },
            },
        ];
        const tiles = [{}];
        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () =>
            createRoot(container).render(<SwaggerUI selectedService={service} tiles={tiles} />, container)
        );
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
            defaultApiVersion: 0,
        };
        service1.apis = [
            {
                default: { apiId: 'enabler' },
                codeSnippet: {
                    codeBlock: 'code',
                    endpoint: '/test',
                    language: 'java',
                },
            },
        ];
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
            defaultApiVersion: 0,
        };

        service2.apis = [
            {
                default: { apiId: 'enabler' },
                codeSnippet: {
                    codeBlock: 'code2',
                    endpoint: '/test2',
                    language: 'python',
                },
            },
        ];
        const container = document.createElement('div');
        document.body.appendChild(container);
        const tiles = [{}];
        await act(async () =>
            createRoot(container).render(<SwaggerUI selectedService={service1} tiles={tiles} />, container)
        );
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint1}`);
        await act(async () =>
            createRoot(container).render(<SwaggerUI selectedService={service2} tiles={tiles} />, container)
        );
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint2}`);
    });

    it('should get snippet from selectedVersion and render swagger', async () => {
        const endpoint1 = '/oldenabler/api/v1';
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
            defaultApiVersion: 0,
        };
        service1.apis = [
            {
                default: { apiId: 'enabler' },
            },
        ];
        service1.apis[0].codeSnippet = {
            codeBlock: 'code',
            endpoint: '/test',
            language: 'java',
        };

        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service1} selectedVersion="0" />
            </div>
        );
        const swaggerDiv = wrapper.find('#swaggerContainer');

        expect(swaggerDiv.length).toEqual(0);
    });

    it('should not create element if api portal disabled and element does not exist', () => {
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
        service.apis = {
            codeSnippet: {
                codeBlock: 'code',
                endpoint: '/test',
                language: 'java',
            },
        };
        jest.spyOn(document, 'getElementById').mockImplementation(() => null);
        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service} />
            </div>
        );
        const swaggerDiv = wrapper.find('span');

        expect(swaggerDiv).toBeDefined();
    });

    it('should not create element api portal disabled and span already exists', () => {
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
        service.apis = {
            codeSnippet: {
                codeBlock: 'code',
                endpoint: '/test',
                language: 'java',
            },
        };
        jest.spyOn(document, 'getElementById').mockImplementation(() => <span id="filter-label" />);
        const createElement = jest.spyOn(document, 'createElement');
        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service} />
            </div>
        );
        const swaggerDiv = wrapper.find('span');

        expect(swaggerDiv.length).toEqual(0);
        expect(createElement).not.toHaveBeenCalled();
    });
});
