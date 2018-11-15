/* eslint-disable no-undef */
import puppeteer from 'puppeteer';

let browser;
let page;
const baseUrl = process.env.REACT_APP_CATALOG_URL_TEST;
console.log('API Catalog Base URL:', baseUrl);
const loginUrl = `${baseUrl}/#/login`;
const dashboardUrl = `${baseUrl}/#/dashboard`;
const defaultDetailPageUrl = `${baseUrl}/#/tile/apimediationlayer`;
const apiCatalogDetailPageUrl = `${baseUrl}/#/tile/apimediationlayer/apicatalog`;

beforeAll(async () => {
    browser = await puppeteer.launch({
        headless: false,
        ignoreHTTPSErrors: true,
        dumpio: true,
        args: ['--no-sandbox', '--ignore-certificate-errors'],
    });
    page = await browser.newPage();
}, 60e3);

afterAll(() => browser.close());

beforeEach(() => {
    page.goto(dashboardUrl);
    window.jasmine.DEFAULT_TIMEOUT_INTERVAL = 60000;
});

describe('>>> e2e tests', async () => {
    it('Should login and navigate to dashboard', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(loginUrl)]);
        await page.waitForSelector('[data-testid="username"]');
        await page.waitForSelector('[data-testid="password"]');
        await page.click('[data-testid="submit"]');
        await page.waitForSelector('div.filtering-container > h2');
        const dashboardTitle = await page.$('div.filtering-container > h2');
        const dashboardTitleText = await page.evaluate(el => el.innerText, dashboardTitle);

        expect(page.url()).toBe(dashboardUrl);
        expect(dashboardTitleText).toBe('Available API services');
    });

    it('Should display product title', async () => {
        const [res] = await Promise.all([
            page.waitForNavigation(),
            page.goto(dashboardUrl),
            page.waitForSelector('div.product-name > span > span > a > h3'),
            page.waitForSelector('div.filtering-container > h2'),
        ]);
        const productTitle = await page.$('div.product-name > span > span > a > h3');
        const productTitleText = await page.evaluate(el => el.innerText, productTitle);
        const dashboardTitle = await page.$('div.filtering-container > h2');
        const dashboardTitleText = await page.evaluate(el => el.innerText, dashboardTitle);

        expect(page.url()).toBe(dashboardUrl);
        expect(dashboardTitleText).toBe('Available API services');
        expect(productTitleText).toBe('API Catalog');
    });

    it('Should display Dashboard title', async () => {
        const dashboardTitle = await page.$('div.filtering-container > h2');
        const dashboardTitleText = await page.evaluate(el => el.innerText, dashboardTitle);
        expect(dashboardTitleText).toBe('Available API services');
    });

    it('Should display API Catalog Tile', async () => {
        let selector = '';
        if (baseUrl === 'http://localhost:3000')
            selector = 'div.css-1lfkm6f-CardRow-CardTitle:nth-child(1) > div > div > h3';
        else selector = 'div.apis > div > div:nth-child(2) > div.css-1wosmm > div > div > h3';
        await page.waitForSelector(selector);
        const catalogTile = await page.$(selector);
        const catalogTileText = await page.evaluate(el => el.innerText, catalogTile);
        expect(catalogTileText).toBe('API Mediation Layer API');
    });

    it('Should filter tiles', async () => {
        page.type('[data-testid=search-bar]', 'api mediation layer for z/os');
        const filteredTiles = await page.$('div.grid-tile.pop > div > div > div > h3');

        await page.waitFor(2000);
        const tileHeader = await page.evaluate(el => el.innerText, filteredTiles);

        expect(tileHeader).toBe('API Mediation Layer API');
    });

    it('Should click on API Catalog Tile and navigate correctly to the detail page', async () => {
        const [res] = await Promise.all([
            page.waitForNavigation(),
            page.waitForSelector('div.apis > div > div:nth-child(2)'),
            page.click('div.apis > div > div:nth-child(2)'),
            page.waitForNavigation(),
        ]);
        expect(page.url()).toBe(defaultDetailPageUrl);
    });

    it('Should display message if no tiles were found', async () => {
        page.type('[data-testid=search-bar]', 'Oh freddled gruntbuggly');
        await page.waitForSelector('[data-testid=search-bar]');
        await page.waitForSelector('#search_no_results');
        const message = await page.$('#search_no_results');
        const messageText = await page.evaluate(el => el.innerText, message);
        expect(messageText).toBe('No tiles found matching search criteria');
    });

    it('Should display the tab title and the description in the detail page', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(defaultDetailPageUrl)]);
        await page.waitForSelector('#title');
        await page.waitForSelector('#description');
        const tab = await page.$('#title');
        await page.waitFor(2000);
        const description = await page.$('#description');
        const tabTitleText = await page.evaluate(el => el.innerText, tab);
        const descriptionText = await page.evaluate(el => el.innerText, description);
        expect(tabTitleText).toBe('API Mediation Layer API');
        expect(descriptionText).toBe('The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.');
    });

    it('Should display the back button', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(defaultDetailPageUrl)]);
        await page.waitForSelector('#go-back-button > span > span');
        const backButton = await page.$('#go-back-button > span > span');
        await page.waitFor(2000);
        const backButtonContent = await page.evaluate(el => el.innerText, backButton);
        expect(backButtonContent).toBe('Back');
    });

    xit('Should display the Gateway service title, URL and description in Swagger', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(defaultDetailPageUrl)]);
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > a > span'
        );
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        const serviceTitle = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitFor(2000);
        const serviceUrl = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > a > span'
        );
        const serviceDescription = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        const serviceTitleText = await page.evaluate(el => el.innerText, serviceTitle);
        const serviceUrlValue = await page.evaluate(el => el.innerText, serviceUrl);
        const serviceDescriptionText = await page.evaluate(el => el.innerText, serviceDescription);
        const expectedTitleValue = 'API Gateway\n' + ' 1.0.0 ';
        const expectedDescriptionValue =
            'REST API for the API Gateway service which is a component of the API Mediation Layer. Use this API to access the Enterprise z/OS Security Manager to perform tasks such as logging in with mainframe credentials and checking authorization to mainframe resources.';
        expect(serviceTitleText).toBe(expectedTitleValue);
        expect(serviceUrlValue).toBe(' /api/v1/apicatalog/apidoc/gateway/v1 ');
        expect(serviceDescriptionText).toBe(expectedDescriptionValue);
    });

    it('Should display the API Catalog service title, URL and description in Swagger', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(apiCatalogDetailPageUrl)]);
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        const serviceTitle = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitFor(2000);
        const serviceUrl = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > a > span'
        );
        const serviceDescription = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        const serviceTitleText = await page.evaluate(el => el.innerText, serviceTitle);
        const serviceDescriptionText = await page.evaluate(el => el.innerText, serviceDescription);
        const expectedTitleValue = 'API Catalog\n' + ' 1.0.0 ';
        const expectedDescriptionValue =
            'REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.';
        expect(serviceTitleText).toBe(expectedTitleValue);
        expect(serviceDescriptionText).toBe(expectedDescriptionValue);
    });

    it('Should go back to the dashboard page, check the URL and check if the search bar works', async () => {
        await page.goto(defaultDetailPageUrl);
        await page.waitForSelector('#go-back-button');
        const [res] = await Promise.all([page.click('#go-back-button'), page.waitForNavigation()]);
        await Promise.all([
            await page.waitForSelector('[data-testid=search-bar]'),
            await page.type('[data-testid=search-bar]', 'api mediation layer for z/os'),
        ]);
        await page.waitForSelector('div.grid-tile.pop > div > div > div > h3');
        const filteredTiles = await page.$('div.grid-tile.pop > div > div > div > h3');
        const tileHeader = await page.evaluate(el => el.innerText, filteredTiles);
        expect(page.url()).toBe(dashboardUrl);

        expect(tileHeader).toBe('API Mediation Layer API');
    });

    it('Should logout and display the login page', async () => {
        await page.waitForSelector('[data-testid="logout"]');
        const [res] = await Promise.all([page.click('[data-testid="logout"]'), page.waitForNavigation()]);
        await page.waitForSelector('[data-testid="username"]');
        await page.waitForSelector('[data-testid="username"]');
    });
});
