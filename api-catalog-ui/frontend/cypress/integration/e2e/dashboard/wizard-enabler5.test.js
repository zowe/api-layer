/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

function login() {
    cy.visit(`${Cypress.env('catalogHomePage')}/#/`);
    cy.url().should('contain', '/login');

    const username = Cypress.env('username');
    const password = Cypress.env('password');

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('input[name="password"]').type(password);

    cy.get('@submitButton').click();
}

describe('>>> Wizard Dialog test', () => {

    it('Dialog test', () => {
        login();

        cy.get('.header').should('exist');

        // Test fifth enabler
        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(4).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler5.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });
});
