const PATH_TO_VERSION_SELECTORS = '.api-diff-form > span > span > div > div > span'
const PATH_TO_VERSION_SELECTOR_ITEMS = '.api-diff-form > span > div > div > div > span > span > span'

describe('>>> Service version comapre Test', () => {
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

    it('Should show compare tab', () => {
        cy.get('.tabs-container').should('exist');
        cy.get('.tabs-container').should('have.length', 2);
        cy.get('.nav-tab').should('exist');
        cy.get('.nav-tab').should('have.length', 4);
        cy.get('.version-text').should('exist');
        cy.get('.version-text').should('have.length', 3);
        cy.get('.version-text').eq(2).should('contain.text', 'Compare');
    });

    it('Should switch to compare tab when clicked', () => {
        cy.get('.api-diff-container').should('not.exist');
        cy.get('.nav-tab').eq(3).should('contain.text', 'Compare');
        cy.get('.nav-tab').eq(3).click();
        cy.get('.api-diff-container').should('exist');

        cy.get('.api-diff-form').should('exist');
        cy.get('.api-diff-form > p').eq(0).should('exist');
        cy.get('.api-diff-form > p').eq(0).should('contain.text', 'Compare');
        cy.get('.api-diff-form > p').eq(1).should('exist');
        cy.get('.api-diff-form > p').eq(1).should('contain.text', 'with');
        cy.get(PATH_TO_VERSION_SELECTORS).eq(0).should('exist');
        cy.get(PATH_TO_VERSION_SELECTORS).eq(0).should('contain.text', 'Select...');
        cy.get(PATH_TO_VERSION_SELECTORS).eq(1).should('exist');
        cy.get(PATH_TO_VERSION_SELECTORS).eq(1).should('contain.text', 'Select...');
        cy.get('.api-diff-form > button').should('exist');
        cy.get('.api-diff-form > button').should('contain.text', 'Go');
    });

    it('Should display version in selector', () => {
        cy.get('.api-diff-container').should('not.exist');
        cy.get('.nav-tab').eq(3).should('contain.text', 'Compare');
        cy.get('.nav-tab').eq(3).click();

        cy.get(PATH_TO_VERSION_SELECTORS).eq(0).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).should('have.length', 2);
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(0).should('have.text', 'v1');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(1).should('have.text', 'v2');
    });

    it('Should display diff when versions set', () => {
        cy.get('.api-diff-container').should('not.exist');
        cy.get('.nav-tab').eq(3).should('contain.text', 'Compare');
        cy.get('.nav-tab').eq(3).click();

        cy.get(PATH_TO_VERSION_SELECTORS).eq(0).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(0).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(0).click();
        cy.get(PATH_TO_VERSION_SELECTORS).eq(1).click();
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(1).should('exist');
        cy.get(PATH_TO_VERSION_SELECTOR_ITEMS).eq(1).click();
        cy.get('.api-diff-form > button').should('exist');
        cy.get('.api-diff-form > button').click();

        cy.get('.api-diff-content').should('exist');
        cy.get('.api-diff-content > header > h1').should('exist');
        cy.get('.api-diff-content > header > h1').should('contain.text', 'Api Change Log');
        cy.get('.api-diff-content > div > div > h2').should('exist');
        cy.get('.api-diff-content > div > div > h2').should('contain.text', 'What\'s New');
        cy.get('.api-diff-content > div > div > ol > li >span').should('exist');
        cy.get('.api-diff-content > div > div > ol > li >span').should('include.text', '/greeting/{yourName}');
    });
});