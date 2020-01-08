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
    cy.get('#password').type(password);

    cy.get('@submitButton').click();
}

describe('>>> Dashboard test', () => {
    it('dashboard test', () => {
        login();

        cy.url().should('contain', '/dashboard');
        cy.get('.header').should('exist');

        cy.url().should('contain', '/dashboard');
        cy.get('.grid-tile').should('have.length.gte', 1);

        cy.get('.grid-tile-status').should('have.length.gte', 1);

        cy.contains('All services are running').should('exist');

        cy.get('.header').should('exist');

        cy.get('input[data-testid="search-bar"]').should('exist');
        cy.contains('Available API services').should('exist');

        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('API Mediation Layer API');

        cy.get('.grid-tile').should('have.length', 1).should('contain', 'API Mediation Layer API');

        cy.get('@search')
            .clear()
            .should('be.empty');

        cy.get('.grid-tile').should('have.length.gte', 1);

        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('Oh freddled gruntbuggly, Thy micturations are to me, (with big yawning)');

        cy.get('.grid-tile').should('have.length', 0);

        cy.get('#search_no_results')
            .should('exist')
            .should('have.text', 'No tiles found matching search criteria');

        cy.get('@search').clear();

        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('API Mediation Layer API');

        cy.get('.grid-tile').should('have.length', 1);

        cy.get('.clear-text-search').click();

        cy.get('.grid-tile').should('have.length.gte', 1);
        cy.get('@search').should('have.text', '');

        cy.contains('API Mediation Layer API').click();

        cy.url().should('contain', '/tile/apimediationlayer/');
    });
});
