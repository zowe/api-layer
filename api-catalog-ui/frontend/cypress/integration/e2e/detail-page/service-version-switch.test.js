describe('>>> Service version change Test', () => {

    beforeEach(() => {
        cy.visit(`${Cypress.env('catalogHomePage')}/#/login`);

        const username = Cypress.env('username');
        const password = Cypress.env('password');

        cy.get('button[type="submit"').as('submitButton');

        cy.get('#username').type(username);
        cy.get('#password').type(password);

        cy.get('@submitButton').click();

        cy.contains('API Mediation Layer API').click();

        cy.visit(`${Cypress.env('catalogHomePage')}/#/tile/cademoapps/discoverableclient`);
    });

    it('Should contain version tabs', () => {
        cy.get('.tabs-container').should('exist');
        cy.get('.tabs-container').should('have.length', 2);
        cy.get('.nav-tab').should('exist');
        cy.get('.nav-tab').should('have.length', 4);
        cy.get('.version-text').should('exist');
        cy.get('.version-text').should('have.length', 3);
        cy.get('.version-text').eq(0).should('contain.text', 'v1');
        cy.get('.version-text').eq(1).should('contain.text', 'v2');
        cy.get('.version-text').eq(2).should('contain.text', 'Compare');
    });

    it('Should pre select default version', () => {
        cy.get('.version-text').should('exist');
        cy.get('.base-url').should('contain.text', '/discoverableclient/api/v1');
    });

    it('Should change version when clicking version 2', () => {
        cy.get('.nav-tab').eq(2).should('contain.text', 'v2');
        cy.get('.nav-tab').eq(2).click();
        cy.get('.base-url', { timeout: 10000 }).should('contain.text', '/discoverableclient/api/v2');
    });
})