function login() {
    cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

    const username = Cypress.env('username');
    const password = Cypress.env('password');

    cy.get('button[type="submit"').as('submitButton');

    cy.get('#username').type(username);
    cy.get('#password').type(password);

    cy.get('@submitButton').click();
}

function goToTile() {
    cy.contains('API Mediation Layer API').click();

    cy.url().should('contain', ('/tile/apimediationlayer'));
}

describe('>>> Swagger Test', () => {
    it('Should contain try-out button', () => {
        login();
        goToTile();

        cy.get('.opblock-summary').eq(0).click();
        cy.get('.try-out').should('exist');
    });


    it('Should protect endpoint', () => {
        login();
        goToTile();

        cy.get('.authorization__btn').should('exist');

        cy.get('.authorization__btn').eq(0).click();

        cy.get('input[name=username]').type('non-valid');
        cy.get('input[name=password]').type('non-valid');

        cy.contains('Basic authorization').parent().parent().parent().submit();

        cy.get('.close-modal').click();

        cy.get('.opblock-summary').eq(0).click();

        cy.get('.try-out').click();

        cy.get('button.execute').click();

        cy.get('table.live-responses-table')
            .find('.response-col_status')
            .should('contain', '401')
    })
});
