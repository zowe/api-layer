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
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Service Spring Onboarding Enabler sample application API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/service/discoverableclient`);
    });

    it('Should contain version tabs', () => {
        cy.get('.tabs-container').should('not.exist');
        cy.get('.version-text').should('exist');
        cy.get('.version-text').should('have.length', 1);

        cy.get('#version-menu').should('exist').click();

        cy.get('#menu- > div > ul').should('exist');

        cy.get('#menu- > div > ul > li').eq(0).should('contain.text', 'zowe.apiml.discoverableclient.ws v1.0.0');
        cy.get('#menu- > div > ul > li').eq(1).should('contain.text', 'zowe.apiml.discoverableclient.rest v1.0.0');
        cy.get('#menu- > div > ul > li').eq(2).should('contain.text', 'zowe.apiml.discoverableclient.rest v2.0.0');
    });

    it('Should change version when clicking version 2', () => {
        cy.get('.version-text').should('exist');
        cy.get('.servers').contains('discoverableclient/api/v1');

        cy.get('#version-menu').should('exist').click();

        cy.get('#menu- > div > ul > li').eq(2).click();
        cy.get('.servers', {timeout: 10000}).contains('/discoverableclient/api/v2');
    });
});
