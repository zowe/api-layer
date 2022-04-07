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

    it('should test enabler 1', () => {
        login();

        cy.get('.header').should('exist');

        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(0).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler1.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    it('should test enabler 2', () => {
        login();

        cy.get('.header').should('exist');

        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(1).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler2.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    it('should test enabler 3', () => {
        login();

        cy.get('.header').should('exist');

        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(2).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler3.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    it('should test enabler 4', () => {
        login();

        cy.get('.header').should('exist');
        
        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(3).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler4.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    it('should test enabler 5', () => {
        login();

        cy.get('.header').should('exist');

        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(4).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler5.yaml');
        
        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });
});
