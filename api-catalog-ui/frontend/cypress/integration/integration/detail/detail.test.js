/* eslint-disable no-undef */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Detail page test', () => {
    it('successfully visits detail page of API Catalog', () => {
        // cy.visit('/#/tile/apicatalog/apicatalog');
        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/apicatalog/apicatalog`);
        cy.url().should('contain', 'apicatalog/apicatalog');
    });

    it('detail should show expected elements', () => {
        cy.get('.header').should('exist');
        cy.get('#go-back-button').should('exist');
        cy.get('#title')
            .should('exist')
            .should('contain', 'API Catalog');

        cy.get('div > span > span > a')
            .should('exist')
            .should('have.attr', 'href', '/ui/v1/apicatalog');

        cy.get('#description')
            .should('exist')
            .should(
                'contain',
                'API Catalog service to display service details and API documentation for discovered API services.'
            );
        cy.get('a.nav-tab')
            .should('exist')
            .should('contain', 'apicatalog')
            .should('have.length', 1)
            .should('have.class', 'active');
        cy.get('h2.css-1hgnwz2')
            .should('exist')
            .should('have.text', 'API Catalog');
        cy.contains('Service Homepage').should('exist');
        cy.get('#swaggerContainer');
    });

    it('should show the right infoin swagger container', () => {
        cy.get('#swaggerContainer').within($swagger => {
            cy.get('h2.title')
                .should('contain', 'API Catalog')
                .within(title => {
                    cy.contains('1.0.0');
                });
            cy.get('pre.base-url').should('contain', '[ Base URL: ca3x.ca.com:10010/api/v1/apicatalog ]');

            cy.get('h4.opblock-tag').should('have.length', 2);

            cy.get('span.opblock-summary-path').should('not.contain', '/api/v1/apicatalog');
        });
    });
});
