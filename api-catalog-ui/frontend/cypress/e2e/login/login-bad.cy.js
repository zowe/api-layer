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
    beforeEach('succesfully loads login page', () => {
        cy.visit(`${Cypress.env('catalogHomePage')}/`);
    });

    it('should not display header', () => {
        cy.get('.header').should('not.exist');
    });

    it('should fail with wrong credentials', () => {
        const user = { username: 'bad', password: 'bad' };

        cy.get('#username').type(user.username);
        cy.get('input[name="password"]').type(user.password);
        cy.get('button[type="submit"').as('submitButton');
        cy.get('@submitButton').click();

        cy.url().should('contain', '/login');

        cy.get('#error-message').should('exist').should('contain', 'Invalid Credentials');
    });
});
