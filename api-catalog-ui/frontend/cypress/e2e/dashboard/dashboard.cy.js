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

describe('>>> Dashboard test', () => {
    it('dashboard test', () => {
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.get('.header').should('exist');

        cy.url().should('contain', '/dashboard');
        cy.get('.grid-tile').should('have.length.gte', 1);

        cy.get('.grid-tile-status').should('have.length.gte', 1);

        cy.contains('The service is running').should('exist');

        cy.get('.header').should('exist');

        cy.get('#search > div > div > input').should('exist');
        cy.get('#refresh-api-button').should('exist').click();
        cy.get('.Toastify').should('have.length.gte', 1);
        cy.get('.Toastify > div> div')
            .should('have.length', 1)
            .should('contain', 'The refresh of static APIs was successful!');

        cy.get('#search > div > div > input')
            .as('search')
            .type('Oh freddled gruntbuggly, Thy micturations are to me, (with big yawning)');

        cy.get('.grid-tile').should('have.length', 0);

        cy.get('#search_no_results').should('exist').should('have.text', 'No services found matching search criteria');

        cy.get('@search').clear();

        cy.get('#search > div > div > input').as('search').type('API Gateway');

        cy.get('.grid-tile').should('have.length', 1);

        cy.get('.clear-text-search').click();

        cy.get('.grid-tile').should('have.length.gte', 1);
        cy.get('@search').should('have.text', '');

        cy.contains('API Catalog').click();

        cy.get('#root > div > div.content > div.header > div.right-icons > div').should('exist').click();
        cy.get('#user-info-text').should('have.length', 1);
        cy.get('#logout-button').should('have.length', 1).should('contain', 'Log out');
    });
});
