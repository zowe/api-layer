/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

function login() {
    cy.visit(`${Cypress.env('catalogHomePage')}ui/v1/apicatalog/#/`);
    cy.url().should('contain', '/login');

    const username = Cypress.env('username');
    const password = Cypress.env('password');

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('#password').type(password);

    cy.get('@submitButton').click();
}

describe('>>> Detail page test', () => {
    it('Detail page test', () => {
        login();

        cy.contains('API Mediation Layer API').click();

        cy.url().should('contain', ('/tile/apimediationlayer'));

        cy.get('#go-back-button').should('exist');

        cy.get('.api-description-container').should('exist');

    });

    it('Should display the Gateway information in the detail page', () => {

        login();

        cy.visit(`${Cypress.env('catalogHomePage')}#/tile/apimediationlayer/apicatalog`);

        const baseUrl = Cypress.env('baseUrl');

        cy.get('pre.base-url')
            .should('exist')
            .should('contain', `[ Base URL: ${baseUrl.match(/^https?:\/\/([^/?#]+)(?:[/?#]|$)/i)[1]}/api/v1/gateway ]`);

        cy.get('.tabs-container')
            .should('exist')
            .should('have.length', 1)
            .within($el => {
                cy.get('a').should('contain', 'gateway');
            });

        cy.contains('Service Homepage').should('exist');

        cy.get('pre.version').should('contain', '1.1.2');

        cy.contains('Swagger/OpenAPI JSON Document').should('exist');

        cy.get('.opblock-tag-section').should('have.length.gte', 1);
    });
});
