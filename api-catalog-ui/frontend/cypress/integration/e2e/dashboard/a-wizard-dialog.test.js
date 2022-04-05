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
        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);

        cy.get('[role="menu"] > .MuiListItem-button').first().click();
        cy.get('[role="dialog"]').should('exist');
    });
});
