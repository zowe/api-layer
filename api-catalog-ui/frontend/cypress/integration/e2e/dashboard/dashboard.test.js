import { PAUSE } from 'redux-persist';

/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Dashboard test', () => {
    beforeEach(() => {
        cy.request('POST', `${Cypress.env('baseURL')}api/v1/apicatalog/auth/login`, {
            user: Cypress.env('username'),
            password: Cypress.env('password'),
        });
    });

    it('should vidit dashboard', () => {
        cy.visit(`${Cypress.env('baseURL')}ui/v1/apicatalog/#/dashboard`);
        cy.url().should('contain', '/dashboard');
    });

    it('should display tiles on dashboard', () => {
        // cy.visit(`${Cypress.env('baseURL')}dashboard`);
        cy.url().should('contain', '/dashboard');
        cy.get('.grid-tile').should('have.length.gte', 1);
    });

    it('should show all possible statuses', () => {
        cy.get('.grid-tile-status').should('have.length.gte', 1);

        cy.contains('All services are running').should('exist');
    });

    it('should show header', () => {
        cy.get('.header').should('exist');
    });

    it('should show search bar and title', () => {
        cy.get('input[data-testid="search-bar"]').should('exist');
        cy.contains('Available API services').should('exist');
    });

    it('should filter tiles', () => {
        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('API Mediation Layer API');

        cy.get('.grid-tile').should('have.length', 1);

        cy.get('@search')
            .clear()
            .should('be.empty');

        cy.get('.grid-tile').should('have.length.gte', 1);
    });

    it('should display message if no tiles satisfy filter', () => {
        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('Oh freddled gruntbuggly, Thy micturations are to me, (with big yawning)');

        cy.get('.grid-tile').should('have.length', 0);

        cy.get('#search_no_results')
            .should('exist')
            .should('have.text', 'No tiles found matching search criteria');

        cy.get('@search').clear();
    });

    it('search bar clear button should work', () => {
        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('API Mediation Layer API');

        cy.get('.grid-tile').should('have.length', 1);

        cy.get('.clear-text-search').click();

        cy.get('.grid-tile').should('have.length.gte', 1);
        cy.get('@search').should('have.text', '');
    });

    it('should navigate to detail page on tile click', () => {
        cy.contains('API Mediation Layer API').click();

        cy.url().should('contain', '/tile/apicatalog/apicatalog');

        cy.get('#go-back-button')
            .should('exist')
            .click();

        cy.url().should('contain', '/dashboard');
    });
});
