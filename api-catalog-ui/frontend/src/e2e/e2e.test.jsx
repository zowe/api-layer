/* eslint-disable no-undef */
import puppeteer from 'puppeteer';

let browser;
let page;
const baseUrl = process.env.REACT_APP_CATALOG_URL_TEST;
console.log('API Catalog Base URL:', baseUrl);
const username = process.env.REACT_APP_CATALOG_USERNAME;
const password = process.env.REACT_APP_CATALOG_PASSWORD;
console.log('API Catalog User ID:', username);
const loginUrl = `${baseUrl}/#/login`;
const dashboardUrl = `${baseUrl}/#/dashboard`;
const defaultDetailPageUrl = `${baseUrl}/#/tile/apimediationlayer`;
const apiCatalogDetailPageUrl = `${baseUrl}/#/tile/apimediationlayer/apicatalog`;

beforeAll(async () => {
    browser = await puppeteer.launch({
        headless: true,
        ignoreHTTPSErrors: true,
        args: ['--no-sandbox', '--ignore-certificate-errors'],
    });
    page = await browser.newPage();
}, 60e3);

afterAll(() => browser.close());

beforeEach(async () => {
    await page.goto(dashboardUrl);
    window.jasmine.DEFAULT_TIMEOUT_INTERVAL = 60000;
});

describe('>>> e2e tests', () => {
    it('Should display error message if login credentials are not valid', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(loginUrl)]);
        await page.waitForSelector('[data-testid="username"]');
        await page.waitForSelector('[data-testid="password"]');
        await page.type('[data-testid="username"]', 'wrongusername'),
            await page.type('[data-testid="password"]', 'wrongpassword'),
            await page.click('[data-testid="submit"]');
        await page.waitForSelector('#error-message > p');
        const messageLabel = await page.$('#error-message > p');
        const messageLabelText = await page.evaluate(el => el.innerText, messageLabel);

        expect(messageLabelText).toBe('Invalid username or password ZWEAS120E');
    });

    it('Should login and navigate to dashboard', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(loginUrl)]);
        await page.waitForSelector('[data-testid="username"]');
        await page.waitForSelector('[data-testid="password"]');
        await page.type('[data-testid="username"]', username),
        await page.type('[data-testid="password"]', password),
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
            page.waitForSelector('.header > .product-name > a:nth-child(2) > h3'),
            page.waitForSelector('div.filtering-container > h2'),
        ]);
        const productTitle = await page.$('.header > .product-name > a:nth-child(2) > h3');
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
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(apiCatalogDetailPageUrl)]);
        await page.waitForSelector('#title');
        await page.waitForSelector('#description');
        const tab = await page.$('#title');
        await page.waitFor(2000);
        const description = await page.$('#description');
        const tabTitleText = await page.evaluate(el => el.innerText, tab);
        const descriptionText = await page.evaluate(el => el.innerText, description);

        expect(tabTitleText).toBe('API Mediation Layer API');
        expect(descriptionText).toBe(
            'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.'
        );
    });

    it('Should display the back button', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(defaultDetailPageUrl)]);
        await page.waitForSelector('#go-back-button > span > span');
        const backButton = await page.$('#go-back-button > span > span');
        await page.waitFor(2000);
        const backButtonContent = await page.evaluate(el => el.innerText, backButton);

        expect(backButtonContent).toBe('Back');
    });

    it('Should display the API Catalog service title, URL and description in Swagger', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(apiCatalogDetailPageUrl)]);
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitForSelector(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        await page.waitForSelector('pre.base-url');
        const serviceTitle = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > hgroup > h2'
        );
        await page.waitFor(2000);
        const serviceUrl = await page.$('pre.base-url');
        const serviceDescription = await page.$(
            '#swaggerContainer > div > div:nth-child(2) > div.information-container.wrapper > section > div > div > div > div > p'
        );
        const serviceTitleText = await page.evaluate(el => el.innerText, serviceTitle);
        const serviceDescriptionText = await page.evaluate(el => el.innerText, serviceDescription);
        const serviceUrlText = await page.evaluate(el => el.innerText, serviceUrl);
        const expectedTitleValue = 'API Catalog\n' + ' 1.0.0 ';
        const expectedUrl = `[ Base URL: ${baseUrl.match(/^https?:\/\/([^/?#]+)(?:[/?#]|$)/i)[1]}/api/v1/apicatalog ]`;
        const expectedDescriptionValue =
            'REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.';

        expect(serviceTitleText).toBe(expectedTitleValue);
        expect(serviceDescriptionText).toBe(expectedDescriptionValue);
        expect(serviceUrlText).toBe(expectedUrl);
    });

    it('Should display the service homepage', async () => {
        const [res] = await Promise.all([page.waitForNavigation(), page.goto(apiCatalogDetailPageUrl)]);
        await page.waitForSelector('#root > div > div.content > div.detail-page > div.content-description-container > div > div:nth-child(2) > div > span > span > a');
        const homepageLabel = await page.$('#root > div > div.content > div.detail-page > div.content-description-container > div > div:nth-child(2) > div > span > span > a');
        const homePageLabelContent = await page.evaluate(a => a.href, homepageLabel);

        expect(homePageLabelContent).toBe(baseUrl);
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

    it('Should have session cookie', async () => {
        await page.goto(defaultDetailPageUrl);
        const { cookies } = await page._client.send("Network.getAllCookies", {});

        expect(cookies["0"].expires).toBe(-1);
        expect(cookies["0"].name).toBe("apimlAuthenticationToken");
    });

    it('Should logout and display the login page', async () => {
        await page.waitForSelector('[data-testid="logout"]');
        const [res] = await Promise.all([page.click('[data-testid="logout"]'), page.waitForNavigation()]);
        await page.waitForSelector('[data-testid="username"]');
        await page.waitForSelector('[data-testid="username"]');
    });

    it('Should delete session cookie after logout', async () => {
        await page.goto(defaultDetailPageUrl);
        const { cookies } = await page._client.send("Network.getAllCookies", {});

        expect(cookies.length).toBe(0);
    });
});
