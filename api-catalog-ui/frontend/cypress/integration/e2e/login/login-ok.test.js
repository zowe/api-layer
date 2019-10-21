/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Login ok page test', () => {
    it('succesfully loads login page', () => {
        cy.visit(`${Cypress.env('catalogHomePage')}ui/v1/apicatalog/#/`);
    });

    it('should not display header', () => {
        cy.get('.header').should('not.exist');
    });

    it('should log in user', () => {
        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"')
            .as('submitButton')
            .should('be.disabled');

        cy.get('#username').type(username);
        cy.get('#password').type(password);

        cy.get('@submitButton').should('not.be.disabled');
        cy.get('@submitButton').click();

        cy.url().should('contain', '/dashboard');
        cy.get('.header').should('exist');
    });

    it('should logout', () => {
        cy.get('button[data-testid="logout"]').click();
        cy.contains('API Catalog');
    });
});
