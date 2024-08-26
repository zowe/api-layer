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

const expectedKeyWords = [
    'name',
    'getAllBooks',
    'Effective Java',
    'Hitchhiker\'s Guide to the Galaxy',
    'Down Under'
];
const PATH_TO_SERVICE_DESCRIPTION =
    '#root > div > div.content > div.main > div.main-content2.detail-content > div.content-description-container > div > div > div.header > h6:nth-child(4)';
const PATH_TO_PLAYGROUND_INPUT_TEXTAREA =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(1) > section > div.graphiql-editor > div > div:nth-child(1) > textarea';
const PATH_TO_QUERY_OUTPUT =
    '#graphiql-session > div:nth-child(3) > div > section > div > div.CodeMirror-scroll > div.CodeMirror-sizer > div > div > div > div.CodeMirror-code';
const PATH_TO_DEFAULT_QUERY =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(1) > section > div.graphiql-editor > div > div.CodeMirror-scroll > div.CodeMirror-sizer > div > div > div > div.CodeMirror-code > div > pre > span > span';
const PATH_TO_RUN_QUERY_BUTTON =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(1) > section > div.graphiql-toolbar > button';
const PATH_TO_ADD_TAB_BUTTON =
    '#graphiql-container > div > div.graphiql-main > div.graphiql-sessions > div.graphiql-session-header > div > button';
const PATH_TO_REMOVE_SPECIFIC_TAB_BUTTON =
    '#graphiql-container > div > div.graphiql-main > div.graphiql-sessions > div.graphiql-session-header > ul > li.graphiql-tab.graphiql-tab-active > button.graphiql-un-styled.graphiql-tab-close';
const PATH_TO_VARIABLES_INPUT_TEXTAREA =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(3) > section > div:nth-child(1) > div > div:nth-child(1) > textarea';
const PATH_TO_VARIABLE_DATA =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(3) > section > div:nth-child(1) > div';
const PATH_TO_HEADER_INPUT_TEXTAREA =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(3) > section > div:nth-child(2) > div > div:nth-child(1) > textarea';
const PATH_TO_HEADER_DATA =
    '#graphiql-session > div:nth-child(1) > div > div:nth-child(3) > section > div:nth-child(2) > div > div.CodeMirror-scroll > div.CodeMirror-sizer > div > div > div > div.CodeMirror-code';
const PATH_TO_PLAYGROUND_INPUT_DATA = '#graphiql-session > div:nth-child(1) > div > div:nth-child(1) > section > div.graphiql-editor > div > div.CodeMirror-scroll > div.CodeMirror-sizer > div > div > div > div.CodeMirror-code';

function login() {
    cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

    const username = Cypress.env('username');
    const password = Cypress.env('password');

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('input[name="password"]').type(password);

    cy.get('@submitButton').click();
}

describe('>>> GraphiQL Playground page test', () => {
    it('Detail page test', () => {
        login();

        cy.get('#grid-container').contains('Discoverable client with GraphQL').click();

        cy.url().should('contain', '/service/graphqlclient');

        cy.get('#go-back-button').should('exist');

        cy.get('.api-description-container').should('exist');
    });

    it('Should display the service information in the detail page', () => {
        login();

        cy.contains('Discoverable client with GraphQL').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/service/graphqlclient`);

        cy.get('.tabs-container').should('not.exist');

        cy.get('.serviceTab').should('exist').and('contain', 'Discoverable client with GraphQL');

        cy.contains('Service Homepage').should('exist');

        cy.contains('Swagger/OpenAPI JSON Document').should('not.exist');

        cy.contains('Service ID and Version').should('not.exist');

        cy.get('#version-div').should('not.exist');

        cy.get(PATH_TO_SERVICE_DESCRIPTION)
            .should('exist')
            .should(
                'contain',
                'Sample for data demonstration using GraphiQL Playround.'
            );
        cy.get('#swagger-label').should('contain', 'GraphQL');

        cy.get(`a[href="#/service/graphqlclient"]`).should('have.class', 'Mui-selected');
    });

    it('Should display the GraphiQL Playground', () => {
        login();

        cy.contains('Discoverable client with GraphQL').click();

        cy.get('#graphiql-container').should('exist');

        cy.get(PATH_TO_PLAYGROUND_INPUT_TEXTAREA).should('be.visible');

        cy.get(PATH_TO_DEFAULT_QUERY)
            .should('exist')
            .should('be.visible')
            .and('have.text', '# Write your query here!');

        const query = '{ "query": "{ hello }" }';
        cy.get(PATH_TO_PLAYGROUND_INPUT_TEXTAREA)
            .first()
            .focus()
            .type('{ctrl}a')
            .type('{del}')
            .type(query, { parseSpecialCharSequences: false });

        cy.get(PATH_TO_PLAYGROUND_INPUT_DATA)
            .then($container => {
                const text = $container.text().trim();
                expect(text).to.include('{ "query": "{ hello }" }');
            })
    })

    it('Should run query', () => {
        login()

        cy.contains('Discoverable client with GraphQL').click();

        const query = 'query {' +
            'getAllBooks{' +
            'name' +
            ' }'
            ;

        cy.get(PATH_TO_PLAYGROUND_INPUT_TEXTAREA)
            .first()
            .focus()
            .type('{ctrl}a')
            .type('{del}')
            .type(query, { parseSpecialCharSequences: false });

        cy.get(PATH_TO_RUN_QUERY_BUTTON).click();

        cy.get('span.cm-def').should('contain.text', 'data');

        cy.get(PATH_TO_QUERY_OUTPUT)
            .then($container => {
                const text = $container.text().trim();
                expectedKeyWords.forEach(word => {
                    expect(text).to.include(word);
                });
            })
    })

    it('Should add and remove a tab in the playground', () => {
        login()

        cy.contains('Discoverable client with GraphQL').click();

        cy.get(PATH_TO_ADD_TAB_BUTTON)
            .click()

        cy.get('#graphiql-session-tab-1')
            .should('exist')
            .should('contain.text', 'My Query 2');

        cy.get(PATH_TO_REMOVE_SPECIFIC_TAB_BUTTON)
            .click()

        cy.get('#graphiql-session-tab-1')
            .should('not.exist');
    })

    it('Should open the Documentation Explorer', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[aria-label="Show Documentation Explorer"]').click();

        cy.get('.graphiql-doc-explorer').should('be.visible');

        cy.get('.graphiql-markdown-description').should('contain', 'A GraphQL schema provides a root type for each kind of operation.')
    });

    it('Should open the History window', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[aria-label="Show History"]').click();

        cy.get('.graphiql-history')
            .should('be.visible')
            .should('contain', 'History');
    });

    it('Refetch button is present', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[aria-label="Re-fetch GraphQL schema"]').should('be.visible');
    });

    it('Should open the short keys dialog', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[aria-label="Open short keys dialog"]').click();

        cy.get('.graphiql-dialog-header h2')
            .should('be.visible')
            .should('contain', 'Short Keys');
    });

    it('Should open the short keys dialog', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[aria-label="Open settings dialog"]').click();

        cy.get('.graphiql-dialog-header h2')
            .should('be.visible')
            .should('contain', 'Settings');
    });

    it('Variable usage', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[data-name="variables"]').click();

        cy.get('.graphiql-editor-tool').should('be.visible');

        const variable = '{"id" :"book-1"}';

        cy.get(PATH_TO_VARIABLES_INPUT_TEXTAREA)
                .first()
                .focus()
                .type(variable, { parseSpecialCharSequences: false });

        cy.get(PATH_TO_VARIABLE_DATA)
            .then($container => {
                const text = $container.text().trim();
                expect(text).to.include(variable);
            })
    });

    it('Header usage', () => {
        login()
        cy.contains('Discoverable client with GraphQL').click();

        cy.get('[data-name="headers"]').click();

        cy.get('.graphiql-editor-tool').should('be.visible');

        const header = '{"X-Custom-Header": "CustomValue"}';

        cy.get(PATH_TO_HEADER_INPUT_TEXTAREA)
            .first()
            .focus()
            .type(header, { parseSpecialCharSequences: false });

        cy.get(PATH_TO_HEADER_DATA)
            .then($container => {
                const text = $container.text().trim();
                expect(text).to.include(header);
            })
    });
})
