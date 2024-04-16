/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable spaced-comment */

describe('>>> Login through Okta OK', () => {


    it('should log in user and check session cookie', () => {

        cy.visit(`${Cypress.env('gatewayOktaRedirect')}`);

        const username = Cypress.env('OKTA_USERNAME');
        const password = Cypress.env('OKTA_PASSWORD');

        cy.get('form span.o-form-input-name-identifier input').type(username);
        cy.get('form input[type="password"]').type(password);

        cy.get('form input.button-primary').should('not.be.disabled');
        cy.get('form input.button-primary').click();

        cy.location('href').should('contain', '/oauth2/v1/authorize')

        cy.url().should('contain', 'https://localhost:10023/application');

        cy.getCookie('apimlAuthenticationToken').should('exist');

    });


});
