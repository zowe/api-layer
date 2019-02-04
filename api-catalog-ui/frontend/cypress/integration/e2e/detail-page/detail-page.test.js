/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

function login() {
    cy.visit(`${Cypress.env('baseURL')}ui/v1/apicatalog/#/`);
    cy.url().should('contain', '/login');

    const username = Cypress.env('username') || 'aicji01';
    const password = Cypress.env('password') || 'tr33iola';

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('#password').type(password);

    cy.get('@submitButton').click();
}

describe('>>> Detail page test', () => {
    it('Detail page test', () => {
        login();

        cy.contains('API Mediation Layer API').click();

        cy.url().should('contain', '/tile/apimediationlayer/apicatalog');

        cy.get('#go-back-button').should('exist');

        cy.get('.api-description-container').should('exist');

        cy.get('.tabs-container')
            .should('exist')
            .should('have.length', 1)
            .within($el => {
                cy.get('a').should('contain', 'apicatalog');
            });

        cy.contains('Service Homepage').should('exist');

        cy.get('pre.version').should('contain', '1.0.0');

        cy.contains('Swagger/OpenAPI JSON Document').should('exist');

        cy.get('.opblock-tag-section').should('have.length.gte', 1);
    });
});
