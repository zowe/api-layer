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
    })

    it('Should contain version buttons', () => {
        cy.get('.version-selection-container').should('exist');
        cy.get('.version-selector').should('exist');
        cy.get('.version-selector').should('have.length', 2);
    })

    it('Should pre select default version', () => {
        cy.get('.version-selector').should('exist');
        cy.get('.base-url').should('contain.text', '/discoverableclient/api/v1')
    })

    it('Should change version when clicking version 2', () => {
        cy.get('.version-selector').last().should('contain.text', 'v2');
        cy.get('.base-url', { timeout: 10000 }).should('contain.text', '/discoverableclient/api/v1');
    })
})