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

function login() {
    const username = Cypress.env('username');
    const password = Cypress.env('password');

    cy.get('button[type="submit"').as('submitButton').should('exist');

    cy.get('#username').type(username);
    cy.get('#password').type(password);

    cy.get('@submitButton').should('not.be.disabled');
    cy.get('@submitButton').click();
}

describe('>>> Login page tests', () => {
    describe('>>> Login ok page test', () => {
        beforeEach('succesfully loads login page', () => {
            cy.visit(`${Cypress.env('metricsHomePage')}`);
        });

        it('should not display header', () => {
            cy.get('.header').should('not.exist');
        });

        it('should log in user and check session cookie', () => {
            login();

            cy.url().should('contain', '/dashboard');
            cy.get('.header').should('exist');

            cy.getCookie('apimlAuthenticationToken').should('exist');
        });

        it('should logout and delete session cookie', () => {
            login();

            cy.get('#logout').click();
            cy.contains('Metrics Service');

            cy.getCookie('apimlAuthenticationToken').should('not.exist');
        });
    });

    describe('>>> Login bad test', () => {
        beforeEach('succesfully loads login page', () => {
            cy.visit(`${Cypress.env('metricsHomePage')}/`);
        });

        it('should not display header', () => {
            cy.get('.header').should('not.exist');
        });

        it('should fail with wrong credentials', () => {
            const user = { username: 'bad', password: 'bad' };

            cy.get('#username').type(user.username);
            cy.get('#password').type(user.password);
            cy.get('button[type="submit"').as('submitButton');
            cy.get('@submitButton').click();

            cy.url().should('contain', '/login');

            cy.get('[id="errormessage"]').should('exist').should('contain', '(ZWEAS120E) Invalid username or password');
        });
    });
});
