/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import countAdditionalContents, { customUIStyle } from './utilFunctions';

describe('>>> Util Functions tests', () => {
    function mockFetch() {
        return jest.fn().mockImplementation(() =>
            Promise.resolve({
                ok: true,
                headers: { get: () => 'image/png', 'Content-Type': 'image/png' },
                blob: () => Promise.resolve(new Blob(['dummy-image-data'], { type: 'image/png' })),
            })
        );
    }
    beforeEach(() => {
        document.body.innerHTML = `
      <div id="separator2"></div>
      <div id="go-back-button"></div>
      <div id="title"></div>
      <div id="swagger-label"></div>
      <div class="header"></div>
      <div class="apis"></div>
      <div class="content"></div>
      <div id="description"></div>
      <div id="logo"></div>
      <div id="internal-link"></div>
      <div id="product-title"></div>
      <div id="onboard-wizard-button">
        <span class="MuiButton-label"></span>
      </div>
      <div id="refresh-api-button">
        <span class="MuiIconButton-label"></span>
      </div>
      <p id="tileLabel"></p>
    `;
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });
    it('should count medias', () => {
        const service = {
            id: 'service',
            hasSwagger: false,
            useCases: ['usecase1', 'usecase2'],
            tutorials: [],
            videos: [],
        };
        expect(countAdditionalContents(service)).toEqual({
            hasSwagger: false,
            tutorialsCounter: 0,
            useCasesCounter: 2,
            videosCounter: 0,
        });
    });

    it('should apply UI changes', async () => {
        const uiConfig = {
            logo: '/path/img.png',
            headerColor: 'red',
            backgroundColor: 'blue',
            fontFamily: 'Arial',
            textColor: 'white',
        };

        global.URL.createObjectURL = jest.fn().mockReturnValue('img-url');
        global.fetch = mockFetch();
        await customUIStyle(uiConfig);
        const logo = document.getElementById('logo');
        const header = document.getElementsByClassName('header')[0];
        const divider = document.getElementById('separator2');
        const title = document.getElementById('title');
        const swaggerLabel = document.getElementById('swagger-label');
        const logoutButton = document.getElementById('go-back-button');
        const homepage = document.getElementsByClassName('apis')[0];
        const detailPage = document.getElementsByClassName('content')[0];
        const description = document.getElementById('description');
        const link = document.querySelector("link[rel~='icon']");
        const wizardButton = document.querySelector('#onboard-wizard-button > span.MuiButton-label');
        const refreshButton = document.querySelector('#refresh-api-button > span.MuiIconButton-label');
        expect(logo.src).toContain('img-url');
        expect(link.href).toContain('img-url');
        expect(header.style.getPropertyValue('background-color')).toBe('red');
        expect(divider.style.getPropertyValue('background-color')).toBe('red');
        expect(title.style.getPropertyValue('color')).toBe('red');
        expect(swaggerLabel.style.getPropertyValue('color')).toBe('red');
        expect(wizardButton.style.getPropertyValue('color')).toBe('red');
        expect(refreshButton.style.getPropertyValue('color')).toBe('red');
        expect(logoutButton.style.getPropertyValue('color')).toBe('red');
        expect(homepage.style.backgroundColor).toBe('blue');
        expect(homepage.style.backgroundImage).toBe('none');
        expect(detailPage.style.backgroundColor).toBe('blue');
        expect(document.documentElement.style.backgroundColor).toBe('blue');
        expect(description.style.color).toBe('white');
        expect(document.body.style.fontFamily).toBe('Arial');
        // Clean up the mocks
        jest.restoreAllMocks();
        global.fetch.mockRestore();
    });

    it('should handle elements in case of white header', async () => {
        const uiConfig = {
            logo: '/path/img.png',
            headerColor: 'white',
            backgroundColor: 'blue',
            fontFamily: 'Arial',
            textColor: 'black',
            docLink: 'doc|doc.com',
        };

        global.URL.createObjectURL = jest.fn().mockReturnValue('img-url');
        global.fetch = mockFetch();
        await customUIStyle(uiConfig);
        const header = document.getElementsByClassName('header')[0];
        const title = document.getElementById('title');
        const productTitle = document.getElementById('product-title');
        const docLink = document.getElementById('internal-link');
        const swaggerLabel = document.getElementById('swagger-label');
        const wizardButton = document.querySelector('#onboard-wizard-button > span.MuiButton-label');
        const refreshButton = document.querySelector('#refresh-api-button > span.MuiIconButton-label');
        const link = document.querySelector("link[rel~='icon']");
        const tileLabel = document.querySelector('p#tileLabel');
        expect(link.href).toContain('img-url');
        expect(header.style.getPropertyValue('background-color')).toBe('white');
        expect(title.style.getPropertyValue('color')).toBe('black');
        expect(productTitle.style.getPropertyValue('color')).toBe('black');
        expect(docLink.style.getPropertyValue('color')).toBe('black');
        expect(swaggerLabel.style.getPropertyValue('color')).toBe('black');
        expect(wizardButton.style.getPropertyValue('color')).toBe('black');
        expect(refreshButton.style.getPropertyValue('color')).toBe('black');
        expect(tileLabel.style.getPropertyValue('font-family')).toBe('Arial');
        expect(document.documentElement.style.backgroundColor).toBe('blue');
        // Clean up the mocks
        jest.restoreAllMocks();
        global.fetch.mockRestore();
    });

    it('should return network error when fetching image', async () => {
        const uiConfig = {
            logo: '/wrong-path/img.png',
        };

        global.fetch = () => Promise.resolve({ ok: false, status: 404 });
        await expect(customUIStyle(uiConfig)).rejects.toThrow('Network response was not ok');
    });
});
