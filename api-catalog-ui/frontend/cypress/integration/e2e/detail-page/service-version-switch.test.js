/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
describe('>>> Service version change Test', () => {
    beforeEach(() => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton');

        cy.get('#username').type(username);
        cy.get('input[name="password"]').type(password);

        cy.get('@submitButton').click();

        cy.contains('API Mediation Layer API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/cademoapps/discoverableclient`);
    });

    it('Should contain version tabs', () => {
        cy.get('.tabs-container').should('exist');
        cy.get('.tabs-container').should('have.length', 2);
        cy.get('.nav-tab').should('exist');
        cy.get('.nav-tab').should('have.length', 5);
        cy.get('.version-text').should('exist');
        cy.get('.version-text').should('have.length', 4);
        cy.get('.version-text').eq(0).should('contain.text', 'zowe.apiml.discoverableclient.ws v1.0.0');
        cy.get('.version-text').eq(1).should('contain.text', 'zowe.apiml.discoverableclient.rest v1.0.0');
        cy.get('.version-text').eq(3).should('contain.text', 'Compare');
    });

    it('Should pre select default version', () => {
        cy.get('.version-text').should('exist');
        cy.get('.servers').contains('discoverableclient/api/v1');
    });

    it('Should change version when clicking version 2', () => {
        cy.get('.nav-tab').eq(2).should('contain.text', 'zowe.apiml.discoverableclient.rest v1.0.0');
        cy.get('.nav-tab').eq(3).click();
        cy.get('.servers', { timeout: 10000 }).contains('/discoverableclient/api/v2');
    });
});
