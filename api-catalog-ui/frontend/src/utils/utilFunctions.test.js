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
    beforeEach(() => {
        document.body.innerHTML = `
      <div id="separator2"></div>
      <div id="go-back-button"></div>
      <div id="title"></div>
      <div id="swagger-label"></div>
      <div class="header"></div>
    `;
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });
    it('should count medias', () => {
        const service = {
            id: 'service',
            useCases: ['usecase1', 'usecase2'],
            tutorials: [],
            videos: [],
        };
        expect(countAdditionalContents(service)).toEqual({ tutorialsCounter: 0, useCasesCounter: 2, videosCounter: 0 });
    });

    it('should apply UI changes', () => {
        const uiConfig = {
            headerColor: 'red',
        };

        customUIStyle(uiConfig);
        const header = document.getElementsByClassName('header')[0];
        const divider = document.getElementById('separator2');
        const title = document.getElementById('title');
        const swaggerLabel = document.getElementById('swagger-label');
        const logoutButton = document.getElementById('go-back-button');

        expect(header.style.getPropertyValue('background-color')).toBe('red');
        expect(divider.style.getPropertyValue('background-color')).toBe('red');
        expect(title.style.getPropertyValue('color')).toBe('red');
        expect(swaggerLabel.style.getPropertyValue('color')).toBe('red');
        expect(logoutButton.style.getPropertyValue('color')).toBe('red');
    });
});
