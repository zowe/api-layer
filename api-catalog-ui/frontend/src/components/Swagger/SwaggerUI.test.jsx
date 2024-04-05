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
import { describe, expect, it, jest } from '@jest/globals';
import SwaggerUI from './SwaggerUI';

describe('>>> Swagger component tests', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    beforeEach(() => {
        process.env.REACT_APP_API_PORTAL = false;
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

        await act(async () => render(<SwaggerUI selectedService={service} tiles={tiles} />, container));
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
        await act(async () => render(<SwaggerUI selectedService={service1} tiles={tiles} />, container));
        expect(container.textContent).toContain(`Servershttp://localhost${endpoint1}`);
        await act(async () => render(<SwaggerUI selectedService={service2} tiles={tiles} />, container));
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
        const container = document.createElement('div');
        document.body.appendChild(container);

        await act(async () => render(<SwaggerUI selectedService={service1} selectedVersion="0" />, container));
        expect(container).not.toBeNull();
    });

    const title = {
        querySelector: jest.fn(),
        appendChild: jest.fn(),
    };
    const divInfo = {
        querySelector: jest.fn(),
    };

    it('should replace inner element in title with version', () => {
        process.env.REACT_APP_API_PORTAL = true;
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
        const querySelectorSpy = jest.spyOn(document, 'querySelector').mockImplementation(() => divInfo);
        const querySelectorDivSpy = jest.spyOn(divInfo, 'querySelector').mockImplementation(() => title);
        const querySelectorTitleSpy = jest.spyOn(title, 'querySelector').mockImplementation(() => 'myVersion');
        const titleSpy = jest.spyOn(title, 'appendChild');

        const wrapper = shallow(<SwaggerUI selectedService={service} />);

        wrapper.setProps({ selectedVersion: 'v2' });

        expect(querySelectorSpy).toHaveBeenCalled();
        expect(querySelectorSpy).toHaveBeenCalledWith('.information-container');
        expect(querySelectorDivSpy).toHaveBeenCalled();
        expect(querySelectorDivSpy).toHaveBeenCalledWith('.title');
        expect(querySelectorTitleSpy).toHaveBeenCalled();
        expect(querySelectorTitleSpy).toHaveBeenCalledWith('.version-stamp');
        expect(titleSpy).toHaveBeenCalled();
        expect(titleSpy).toHaveBeenCalledWith('myVersion');
    });

    it('should NOT replace inner element in title with version if information-container missing', () => {
        process.env.REACT_APP_API_PORTAL = true;
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
        const querySelectorSpy = jest.spyOn(document, 'querySelector').mockImplementation(() => null);
        const querySelectorDivSpy = jest.spyOn(divInfo, 'querySelector')
        const querySelectorTitleSpy = jest.spyOn(title, 'querySelector')
        const titleSpy = jest.spyOn(title, 'appendChild');

        const wrapper = shallow(<SwaggerUI selectedService={service} />);

        wrapper.setProps({ selectedVersion: 'v2' });

        expect(querySelectorSpy).toHaveBeenCalled();
        expect(querySelectorSpy).toHaveBeenCalledWith('.information-container');
        expect(querySelectorDivSpy).not.toHaveBeenCalled();
        expect(querySelectorTitleSpy).not.toHaveBeenCalled();
        expect(titleSpy).not.toHaveBeenCalled();
    });

    it('should NOT replace inner element in title with version if title missing', () => {
        process.env.REACT_APP_API_PORTAL = true;
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
        const querySelectorSpy = jest.spyOn(document, 'querySelector').mockImplementation(() => divInfo);
        const querySelectorDivSpy = jest.spyOn(divInfo, 'querySelector').mockImplementation(() => null);
        const querySelectorTitleSpy = jest.spyOn(title, 'querySelector')
        const titleSpy = jest.spyOn(title, 'appendChild');

        const wrapper = shallow(<SwaggerUI selectedService={service} />);

        wrapper.setProps({ selectedVersion: 'v2' });

        expect(querySelectorSpy).toHaveBeenCalled();
        expect(querySelectorSpy).toHaveBeenCalledWith('.information-container');
        expect(querySelectorDivSpy).toHaveBeenCalled();
        expect(querySelectorDivSpy).toHaveBeenCalledWith('.title');
        expect(querySelectorTitleSpy).not.toHaveBeenCalled();
        expect(titleSpy).not.toHaveBeenCalled();
    });

    it('should NOT replace inner element in title with version if version missing', () => {
        process.env.REACT_APP_API_PORTAL = true;
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
        const querySelectorSpy = jest.spyOn(document, 'querySelector').mockImplementation(() => divInfo);
        const querySelectorDivSpy = jest.spyOn(divInfo, 'querySelector').mockImplementation(() => title);
        const querySelectorTitleSpy = jest.spyOn(title, 'querySelector').mockImplementation(() => null);
        const titleSpy = jest.spyOn(title, 'appendChild');

        const wrapper = shallow(<SwaggerUI selectedService={service} />);

        wrapper.setProps({ selectedVersion: 'v2' });

        expect(querySelectorSpy).toHaveBeenCalled();
        expect(querySelectorSpy).toHaveBeenCalledWith('.information-container');
        expect(querySelectorDivSpy).toHaveBeenCalled();
        expect(querySelectorDivSpy).toHaveBeenCalledWith('.title');
        expect(querySelectorTitleSpy).toHaveBeenCalled();
        expect(querySelectorTitleSpy).toHaveBeenCalledWith('.version-stamp');
        expect(titleSpy).not.toHaveBeenCalled();
    });

    it('should not replace inner element if api portal disabled', () => {
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
        const querySelectorSpy = jest.spyOn(document, 'querySelector');
        const wrapper = shallow(
            <div>
                <SwaggerUI selectedService={service} />
            </div>
        );
        const swaggerDiv = wrapper.find('span');

        expect(swaggerDiv).toBeDefined();
        expect(querySelectorSpy).not.toHaveBeenCalled();
    });
});
