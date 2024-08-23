/* eslint-disable header/header */
// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This is will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })
import 'cypress-file-upload';

Cypress.Commands.add("login", (username, password) => {
    cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);
    cy.url().should('contain', '/login');

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('input[name="password"]').type(password);

    cy.get('@submitButton').click();
    cy.contains('Version: ');
});
