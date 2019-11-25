/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Login bad test', () => {
    it('succesfully loads login page', () => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);
    });

    it('should not display header', () => {
        cy.get('.header').should('not.exist');
    });

    it('should fail with wrong credentials', () => {
        const user = { username: 'bad', password: 'bad' };

        cy.get('#username').type(user.username);
        cy.get('#password').type(user.password);

        cy.get('button[type="submit"').as('submitButton');
        cy.get('@submitButton').should('not.be.disabled');
        cy.get('@submitButton').click();

        cy.url().should('contain', '/login');

        cy.get('.error-message-content')
            .should('exist')
            .should('contain', 'Invalid username or password ZWEAS120E');
    });
});
