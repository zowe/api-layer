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
/// <reference types="Cypress" />

describe('>>> Login ok page test', () => {
    beforeEach('succesfully loads login page', () => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/`);
    });

    it('should not display header', () => {
        cy.get('.header').should('not.exist');
    });

    it('should log in user and check session cookie', () => {
        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton').should('exist');

        cy.get('#username').type(username);
        cy.get('input[name="password"]').type(password);

        cy.get('@submitButton').should('not.be.disabled');
        cy.get('@submitButton').click();

        cy.url().should('contain', '/dashboard');
        cy.get('.header').should('exist');

        cy.getCookie('apimlAuthenticationToken').should('exist');
        cy.get('button[data-testid="logout-menu"]').click();
        cy.get('li[data-testid="logout"]').click();
        cy.contains('API Catalog');

        cy.getCookie('apimlAuthenticationToken').should('not.exist');
    });
});
