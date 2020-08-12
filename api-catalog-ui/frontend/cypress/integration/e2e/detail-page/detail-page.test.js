/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

function login() {
    cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

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

        cy.contains('The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.');

    });

    it('Should display the API Catalog service title, URL and description in Swagger', () => {

        login();

        cy.contains('API Mediation Layer API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/apimediationlayer/apicatalog`);

        const baseUrl = `${Cypress.env('catalogHomePage')}`;

        cy.get('pre.base-url')
            .should('exist')
            .should('contain', `[ Base URL: ${baseUrl.match(/^https?:\/\/([^/?#]+)(?:[/?#]|$)/i)[1]}/api/v1/apicatalog ]`);

        cy.get('.tabs-container')
            .should('exist')
            .should('have.length', 1)
            .within($el => {
                cy.get('a').should('contain', 'apicatalog');
            });

        cy.contains('Service Homepage').should('exist');

        cy.get('#root > div > div.content > div.detail-page > div.content-description-container > div > div:nth-child(2) > div > span > span > a')
            .should('have.attr', 'href')
            .should('contain', `${baseUrl.match(/^https?:\/\/([^/?#]+)(?:[/?#]|$)/i)[1]}/apicatalog/ui/v1`);

        cy.get('pre.version').should('contain', '1.0.0');

        cy.contains('Swagger/OpenAPI JSON Document').should('exist');

        cy.get('.opblock-tag-section').should('have.length.gte', 1);

    });




    it('Should display the Gateway information in the detail page', () => {

        login();

        cy.contains('API Mediation Layer API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/apimediationlayer/gateway`);

        const baseUrl = `${Cypress.env('catalogHomePage')}`;

        cy.get('#swaggerContainer > div > div:nth-child(2) > div.scheme-container > section > div:nth-child(1) > div > label > select > option')
            .should('exist')
            .should('contain', `${baseUrl.match(/^https?:\/\/([^/?#]+)(?:[/?#]|$)/i)[1]}/api/v1/gateway`);

        cy.get('.tabs-container')
            .should('exist')
            .should('have.length', 1)
            .within($el => {
                cy.get('a').should('contain', 'gateway');
            });

        cy.contains('Service Homepage').should('exist');

        cy.get('pre.version').should('contain', 'OAS3');

        cy.contains('Swagger/OpenAPI JSON Document').should('exist');

        cy.get('.opblock-tag-section').should('have.length.gte', 1);

        cy.get('#root > div > div.content > div.detail-page > div.content-description-container > div > div:nth-child(2) > div > p')
            .should('exist')
            .should('contain', 'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.');

    });

    it('Should go to the detail page, go back to the dashboard page and check if the search bar works', () => {

        login();

        cy.contains('API Mediation Layer API').click();

        cy.url().should('contain', ('/tile/apimediationlayer'));

        cy.get('#go-back-button').should('exist').click();

        cy.get('input[data-testid="search-bar"]').should('exist');
        cy.contains('Available API services').should('exist');

        cy.get('input[data-testid="search-bar"]')
            .as('search')
            .type('API Mediation Layer API');

        cy.get('.grid-tile').should('have.length', 1).should('contain', 'API Mediation Layer API');

    });
});
