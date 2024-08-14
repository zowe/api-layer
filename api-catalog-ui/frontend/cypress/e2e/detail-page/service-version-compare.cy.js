/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/// <reference types="Cypress" />

// api-diff-form is now a floating window.
const PATH_TO_VERSION_SELECTORS = '.api-diff-form > div:nth-child(2) > div > div';
const PATH_TO_VERSION_SELECTORS2 = '.api-diff-form > div:nth-child(4) > div > div';
const PATH_TO_VERSION_SELECTOR_ITEMS =
    '#menu- > div.MuiPaper-root.MuiMenu-paper.MuiPopover-paper.MuiPaper-elevation8.MuiPaper-rounded > ul> li';
const PATH_TO_VERSION_SELECTOR_ITEMS2 =
    '#menu- > div.MuiPaper-root.MuiMenu-paper.MuiPopover-paper.MuiPaper-elevation8.MuiPaper-rounded > ul > li:nth-child(1)';

describe('>>> Service version compare Test', () => {
    beforeEach(() => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton');

        cy.get('#username').type(username);
        cy.get('input[name="password"]').type(password);

        cy.get('@submitButton').click();

        cy.contains('Service Spring Onboarding Enabler sample application API').click(); // discoverable client

        cy.visit(`${Cypress.env('catalogHomePage')}/#/service/discoverableclient`);
    });

    it('Should show compare tab', () => {
        // Location of the compare has changed, it's no longer a specific tab
        cy.get('.tabs-container').should('not.exist');
        cy.get('div.MuiTabs-root.custom-tabs.MuiTabs-vertical > div.MuiTabs-scroller.MuiTabs-scrollable > div').should(
            'exist'
        );
        cy.get('div.MuiTabs-flexContainer.MuiTabs-flexContainerVertical') // Select the parent div
            .find('a.MuiTab-root') // Find all the anchor elements within the div
            .should('have.length', 13); // Check if there are 13 anchor elements within the div -- changed from 12 to 13 after adding Discoverable client with GraphQL
        cy.get('.version-text').should('exist');
        cy.get('.version-text').should('contain.text', 'Compare');
    });

    it('Should switch to compare tab when clicked', () => {
        cy.get('.api-diff-container').should('not.exist');
        cy.get('#compare-button > span.MuiButton-label > p').should('contain.text', 'Compare API Versions').click();
        cy.get('.api-diff-container').should('exist');

        cy.get('.api-diff-form').should('exist');
        cy.get('.api-diff-form > div:nth-child(2) > label').eq(0).should('exist').and('contain.text', 'Version');

        cy.get('.api-diff-form > p').eq(0).should('contain.text', 'Compare');
        cy.get('.api-diff-form > p').eq(1).should('exist');
        cy.get('.api-diff-form > p').eq(1).should('contain.text', 'with');

        cy.get('.api-diff-form > div:nth-child(2) > label').eq(0).should('exist').and('contain.text', 'Version');
        cy.get('.api-diff-form > div:nth-child(2) > label').eq(0).should('contain.text', 'Version');
        cy.get('.api-diff-form > div:nth-child(4) > label').eq(0).should('exist');
        cy.get('.api-diff-form > div:nth-child(4) > label').eq(0).should('contain.text', 'Version');
        cy.get('.api-diff-form > button').should('exist');
        cy.get('.api-diff-form > button').should('contain.text', 'Show');
    });

    it('Should display version in selector', () => {
        cy.get('#compare-button > span.MuiButton-label > p').should('contain.text', 'Compare API Versions').click();

        cy.get(PATH_TO_VERSION_SELECTORS).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).should('have.length', 3);
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(0).should('have.text', 'zowe.apiml.discoverableclient.ws v1.0.0');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(1).should('have.text', 'zowe.apiml.discoverableclient.rest v1.0.0');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(2).should('have.text', 'zowe.apiml.discoverableclient.rest v2.0.0');
    });

    it('Should display diff when versions set', () => {
        cy.get('.api-diff-container').should('not.exist');
        cy.get('#compare-button > span.MuiButton-label > p').should('contain.text', 'Compare API Versions').click();

        cy.get(PATH_TO_VERSION_SELECTORS).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(1).click();
        cy.get(PATH_TO_VERSION_SELECTORS2).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS2).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS2).eq(0).click();
        cy.get('.api-diff-form > button').should('exist');
        cy.get('.api-diff-form > button').click();

        cy.get('.api-diff-content').should('exist');
        cy.get('.api-diff-content > header > h1').should('exist');
        cy.get('.api-diff-content > header > h1').should('contain.text', 'Api Change Log');
        cy.get('.api-diff-content > div > div > h2').should('exist');
        cy.get('.api-diff-content > div > div > h2').should('contain.text', "What's New");
        cy.get('.api-diff-content > div > div > ol > li >span').should('exist');
        cy.get('.api-diff-content > div > div > ol > li >span').should('include.text', 'Get a greeting');
    });
});
