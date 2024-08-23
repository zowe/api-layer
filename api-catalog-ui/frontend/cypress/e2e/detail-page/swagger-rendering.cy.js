/*
 *
 *  * This program and the accompanying materials are made available under the terms of the
 *  * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 *  * https://www.eclipse.org/legal/epl-v20.html
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *
 *  * Copyright Contributors to the Zowe Project.
 *
 */

describe("Swagger rendering", () => {

    beforeEach("Login to API Catalog", () => {
        cy.login(Cypress.env('username'), Cypress.env('password'));
    });

    [
        {
            "serviceName": "API Gateway",
            "serviceId": "gateway",
            "serviceHomepage": `${Cypress.env('catalogHomePage')}`.split("/apicatalog")[0]
        },
        {
            "serviceName": "API Catalog",
            "serviceId": "apicatalog",
            "serviceHomepage": `${Cypress.env('catalogHomePage')}`.split("/apicatalog")[0]
        },
        {
            "serviceName": "Mock zOSMF",
            "serviceId": "mockzosmf",
            "serviceHomepage": "https://localhost:10013/"
        },
    ].forEach((service) => {
        it("Rendering Swagger for " + service.serviceName, () => {

            cy.get('#grid-container').contains(service.serviceName).click();

            cy.get('div.tabs-swagger').should('exist');

            cy.get('div.tabs-swagger > div.serviceTab > div.header > h4').should('contain', service.serviceName);

            cy.get('div.tabs-swagger > div.serviceTab > div.header > a:nth-child(2)')
                .as('serviceHomepage').should("contain", "Service Homepage");

            cy.get('@serviceHomepage').should('have.attr', 'title', 'Open Service Homepage')

            cy.get('div.tabs-swagger')
                .find('.apiInfo-item')
                .get('h6:nth-Child(1)')
                .as('basePath');

            cy.get('@basePath')
                .get('label')
                .should('contain', "API Base Path:");

            const regex = new RegExp('^$|\/' + service.serviceId + '\/api(\/v1)?$');
            cy.get('@basePath')
                .get('#apiBasePath').invoke("text").should(text => {
                expect(text).to.match(regex);
            });

            cy.get('div.tabs-swagger')
                .find('.apiInfo-item')
                .get('h6:nth-Child(2)')
                .as('sId');

            cy.get('@sId')
                .get('label')
                .should('contain', "Service ID:");

            cy.get('@sId')
                .get('#serviceId')
                .should('contain', service.serviceId);

            cy.get('#swagger-label')
                .should('exist')
                .should('contain', 'Swagger');

            cy.get('#swaggerContainer').as('swaggerContainer');

            cy.get('@swaggerContainer').should('exist');

            cy.get('@swaggerContainer')
                .get('div.swagger-ui > div:nth-child(2) > div.information-container')
                .should('exist');

            cy.get('@swaggerContainer')
                .get('div.information-container > section > div > div.info > .main')
                .as('mainInfo');

            cy.get('@mainInfo').should('exist');

            cy.get('@mainInfo')
                .get('h2')
                .should('have.class', 'title');

            cy.get('@mainInfo')
                .get('h2')
                .find('pre.version')
                .should('exist');

            cy.get('@swaggerContainer')
                .get('div.swagger-ui > div:nth-child(2) > div.wrapper')
                .should('exist');

            cy.get('@swaggerContainer')
                .get('div.wrapper > section > div > div.apiInfo-item > p > label')
                .as('instanceUrlLabel');

            cy.get('@instanceUrlLabel')
                .should('exist')
                .should('contain', 'Instance URL:');

            cy.get('@swaggerContainer')
                .get('div.swagger-ui > div:nth-child(2) > div.wrapper')
                .should('exist');

            cy.get('@swaggerContainer')
                .get('div.wrapper > section > div > div > div.opblock-tag-section')
                .as('operationBlock');

            cy.get('@operationBlock').should('exist');

            cy.get('@operationBlock')
                .find('.operation-tag-content')
                .should('exist');

            cy.get('@operationBlock')
                .get('.operation-tag-content')
                .find('.opblock')
                .should('exist');

            cy.get('@operationBlock')
                .get('.operation-tag-content > .opblock > .opblock-summary > button:first-child')
                .should('exist');
            cy.get('@operationBlock')
                .get('button:first-child')
                .should('have.class', 'opblock-summary-control');

            cy.get('@serviceHomepage').click();

            cy.url().should('contain', service.serviceHomepage);
        });
    })
});

