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

describe('>>> Login bad test', () => {

    it('should not display header', () => {
        cy.visit(`${Cypress.env('catalogHomePage')}/`);
        cy.get('.header').should('not.exist');
    });

    it('should fail with wrong credentials', () => {
        cy.login('bad', 'bad');

        cy.url().should('contain', '/login');

        cy.get('#error-message').should('exist').should('contain', 'Invalid Credentials');
    });
});
