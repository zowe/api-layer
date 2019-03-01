/* eslint-disable no-undef,new-cap,no-use-before-define */

load('pages/dashboardPage.jsx');

test('Should display tiles in the correct position (desktop resolution)', () = > {
    const driver = createDriver('http://localhost:3000/dashboard', '1920x1080');
const dashboardPage = new DashboardPage(driver);
dashboardPage.tile1.waitToBeShown();
dashboardPage.tile2.waitToBeShown();
dashboardPage.load();
checkLayout({
    driver: driver,
    spec: 'src/responsive-tests/specs/tiles.gspec',
});
driver.quit();
})
;
test('Should display tiles in the correct position (mobile resolution)', () = > {
    const driver = createDriver('http://localhost:3000/dashboard', '640x480');
const dashboardPage = new DashboardPage(driver);
dashboardPage.tile1.waitToBeShown();
dashboardPage.tile2.waitToBeShown();
dashboardPage.load();
checkLayout({
    driver: driver,
    spec: 'src/responsive-tests/specs/tiles.gspec',
    tags: ["mobile"],
});
driver.quit();
})
;
test('Should display tiles in the correct position (tablet resolution)', () = > {
    const driver = createDriver('http://localhost:3000/dashboard');
const dashboardPage = new DashboardPage(driver);
dashboardPage.tile1.waitToBeShown();
dashboardPage.tile2.waitToBeShown();
dashboardPage.load();
checkLayout({
    driver: driver,
    spec: 'src/responsive-tests/specs/tiles.gspec',
    tags: ["tablet"],
});
driver.quit();
})
;
