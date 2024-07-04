/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

describe('>>> Swagger Try Out and Code Snippets Test', () => {
    beforeEach(() => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton');

        cy.get('#username').type(username);
        cy.get('input[name="password"]').type(password);

        cy.get('@submitButton').click();
    });

    [
        {
            tile: 'API Gateway',
            id: 'gateway',
            selectOp: '#operations-Diagnostic-VersionInfoUsingGET',
            auth: true,
        },
    ].forEach((test) => {
        it('Should contain try-out button', () => {
            cy.log(`Visiting ${test.tile}, ${test.id}`);
            cy.contains(test.tile).click();
            cy.visit(`${Cypress.env('catalogHomePage')}/#/service/${test.id}`);
            cy.get('.opblock-summary').eq(0).click();
            cy.get('.try-out').should('exist');
        });

        it('Should protect endpoint', () => {
            if (test.auth) {
                cy.log(`Visiting ${test.tile}, ${test.id}`);
                cy.contains(test.tile).click();
                cy.get('#operations-Security-loginUsingPOST .authorization__btn').should('exist');

                cy.get('#operations-Security-loginUsingPOST .authorization__btn').should('exist');

                cy.get('#operations-Security-loginUsingPOST .authorization__btn').eq(0).click();

                cy.get('input[name=username]').type('non-valid');
                cy.get('input[name=password]').type('non-valid');

                cy.contains('LoginBasicAuth').parent().parent().parent().submit();

                cy.get('.close-modal').click();

                cy.get('#operations-Security-loginUsingPOST .opblock-summary').eq(0).click();

                cy.get('.try-out').click();

                cy.get('button.execute').click();

                cy.get('table.live-responses-table').find('.response-col_status').should('contain', '401');
            }
        });

        it('Should execute request and display basic code snippets', () => {
            cy.log(`Visiting ${test.tile}, ${test.id}`);
            cy.contains(test.tile).click();

            cy.viewport(1400, 980);

            cy.get('.opblock-control-arrow').eq(1).click();
            cy.get(`${test.selectOp} .opblock-control-arrow`).eq(0).click();
            cy.get('.try-out__btn').should('exist');
            cy.get('.try-out__btn').eq(0).click();

            cy.get('.execute').should('exist');
            cy.get('.execute').click();

            // Snippet open/collaptse element exists
            cy.get('.request-snippets > div:nth-child(1) > button:nth-child(2)').should('exist');

            // cURL snippet tab exists
            cy.get('div.btn:nth-child(1) > h4:nth-child(1)').should('contain', 'cURL (CMD)');

            // cURL snippet box exists
            cy.get('.curl').should('exist');
            cy.get('div.curl-command > div:nth-child(1) > div:nth-child(2)').click();
            cy.get('div.curl-command > div:nth-child(3) > pre').should('exist');
        });
    });
});
