/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Login ok page test', () => {
    it('succesfully loads login page', () => {
        // cy.visit('/');
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);
    });

    it('should not display header', () => {
        cy.get('.header').should('not.exist');
    });

    it('should log in user', () => {
        const user = { username: 'user', password: 'user' };


        cy.get('#username').type(user.username);
        cy.get('#password').type(user.password);

        cy.get('button[type="submit"')
            .as('submitButton')
            .should('exist');

        cy.get('@submitButton').should('not.be.disabled');
        cy.get('@submitButton').click();

        cy.url().should('contain', '/dashboard');
        cy.get('.header').should('exist');
    });

    it('should logout', () => {
        cy.get('button[data-testid="logout"]').click();

        cy.url().should('contain', '/login');
        cy.get('.header').should('not.exist');
    });
});
