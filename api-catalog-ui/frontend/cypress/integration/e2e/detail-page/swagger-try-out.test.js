describe('>>> Swagger Test', () => {

    beforeEach(() => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton');

        cy.get('#username').type(username);
        cy.get('#password').type(password);

        cy.get('@submitButton').click();

        cy.contains('API Mediation Layer API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/apimediationlayer/apicatalog`);
    })



    it('Should contain try-out button', () => {
        cy.get('.opblock-summary').eq(0).click();
        cy.get('.try-out').should('exist');
    })


    it('Should protect endpoint', () => {
        cy.get('.authorization__btn').should('exist');

        cy.get('.authorization__btn').eq(0).click()

        cy.get('input[name=username]').type('non-valid');
        cy.get('input[name=password]').type('non-valid');

        cy.contains('Basic authorization').parent().parent().parent().submit();

        cy.get('.close-modal').click()

        cy.get('.opblock-summary').eq(0).click();

        cy.get('.try-out').click();

        cy.get('button.execute').click();

        cy.get('table.live-responses-table')
            .find('.response-col_status')
            .should('contain', '401')

    
    })


})
