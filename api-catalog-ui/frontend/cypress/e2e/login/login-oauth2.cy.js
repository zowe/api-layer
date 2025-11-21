/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable no-undef */

describe('>>> Login through Auth0 OK', () => {
    it('should log in user and check session cookie', () => {

        // === DEBUG POST /u/login ===
        cy.intercept({ method: 'POST', url: '**/u/login**' }, (req) => {
            req.continue((res) => {
                cy.task('log', '=== POST LOGIN REQUEST + RESPONSE ===');
                cy.task('log', JSON.stringify({
                    requestUrl: req.url,
                    requestHeaders: req.headers,
                    requestBody: req.body,
                    responseStatus: res.statusCode,
                    responseHeaders: res.headers,
                    responseBody: res.body
                }, null, 2));
            });
        }).as('auth0Login');


        // === DEBUG GET /authorize ===
        cy.intercept('GET', '**/authorize**', (req) => {
            req.continue((res) => {
                cy.task('log', '=== GET AUTHORIZE RESPONSE ===');
                cy.task('log', JSON.stringify({
                    url: req.url,
                    status: res.statusCode,
                    headers: res.headers
                }, null, 2));
            });
        }).as('authorize');
        cy.intercept('GET', '**', (req) => {
            if (req.response && req.response.statusCode === 302) {
                cy.task('log', '=== REDIRECT 302 === ' + req.url);
            }
        });

        cy.visit(`${Cypress.env('gatewayAuth0Redirect')}`);

        const username = Cypress.env('AUTH0_USERNAME');
        if (!username) {
            cy.log('System env CYPRESS_AUTH0_USERNAME is not set');
        }

        const password = Cypress.env('AUTH0_PASSWORD');
        if (!password) {
            cy.log('System env CYPRESS_AUTH0_PASSWORD is not set');
        }

        // cy.get('form span.o-form-input-name-username input').type(username);
        cy.get('#username').type(username);
        // cy.get('form input[type="password"]').type(password);
        cy.get('#password').type(password);
        cy.task('log', 'CLICK LOGIN NOW');
        cy.get('button[data-action-button-primary="true"]').should('not.be.disabled');
        // cy.get('form input.button-primary').click();
        cy.get('button[data-action-button-primary="true"]').click();
        cy.wait('@auth0Login', { timeout: 15000 });
        cy.wait('@authorize', { timeout: 15000 });
        cy.url().should('contain', '/application');

        cy.getCookie('apimlAuthenticationToken').should('exist');
    });
});
