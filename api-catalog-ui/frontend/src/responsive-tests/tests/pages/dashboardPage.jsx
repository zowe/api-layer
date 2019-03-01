/* eslint-disable no-undef */

this.DashboardPage = $page('Dashboard Page', {
    tile1: 'xpath:  /html/body/div/div/div[4]/div[2]/div/div[2]',
    tile2: 'xpath:  /html/body/div/div/div[4]/div[2]/div/div[3]',

    load() {
        return this.waitForIt();
    },
});
