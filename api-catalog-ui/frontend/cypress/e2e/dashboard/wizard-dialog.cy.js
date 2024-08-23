/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable spaced-comment */
/// <reference types="Cypress" />

describe('>>> Wizard Dialog test', () => {
    it('should test enabler 1', () => {
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Version: ');
        cy.get('.header').should('exist');

        cy.get('#onboard-wizard-button').should('exist').click();
        cy.get('[role="menu"] > .MuiListItem-button').should('have.length', 6);
        cy.get('[role="menu"] > .MuiListItem-button').eq(0).should('contain', 'Plain Java Enabler');
        cy.get('[role="menu"] > .MuiListItem-button').eq(1).should('contain', 'Spring Enabler');
        cy.get('[role="menu"] > .MuiListItem-button').eq(2).should('contain', 'Micronaut Enabler');
        cy.get('[role="menu"] > .MuiListItem-button').eq(3).should('contain', 'Node JS Enabler');
        cy.get('[role="menu"] > .MuiListItem-button').eq(4).should('contain', 'Static Onboarding');
        cy.get('[role="menu"] > .MuiListItem-button').eq(5).should('contain', 'Direct Call to Eureka');

        cy.get('[role="menu"] > .MuiListItem-button').eq(0).click();
        cy.get('[role="dialog"]').should('exist');

        cy.get('#yaml-browser').should('exist');
        cy.get('#yaml-browser').attachFile('enabler-test-files/testEnabler1.yaml');

        cy.get('#yaml-file-text').should('exist');
        cy.get('#wizard-cancel-button').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    it('should test enabler 2', () => {
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Version: ');
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
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Version: ');
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
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Version: ');
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
        cy.login(Cypress.env('username'), Cypress.env('password'));

        cy.contains('Version: ');
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
