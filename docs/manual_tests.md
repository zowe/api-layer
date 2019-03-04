# Manual Smoke Test Guideline

We are currently working on our automated smoke tests. Until then the following manual testing procedure should be followed (takes only 2-3 minutes). They need to run against any supported environment:
 - local computer with Gradle JARs via command line
 - development system on z/OS
 - DVIPA master system (if accessible)
 - services executed from IntelliJ via Run Dashboard
 - z/OS system installed from APAR (in future)

Note 1: Replace ``http://yourhost:10010/`` with the correct URL prefix for your environment.
Note 2: In all the steps above, the loading spinner should display in waiting periods between pages. 


### For API Gateway
Basic routing of an API endpoint (automated by message tests) 
   - Issue an HTTP GET request on ``http://yourhost:10010/api/v1/helloworld/greeting`` (via Curl, or Httpie)
   - Check that the message code is 200 OK


### For API Catalog
- Go to <https://yourhost:10010/ui/v1/apicatalog/>. You will be redirected to the login page (<https://yourhost:10010/ui/v1/apicatalog/#/login>)
    - `Username` and `password` input fields should be displayed, along with the `Sign in` button.
- Insert your mainframe credentials and press the `Sign in` button. After you logged in successfully, you will be redirected to the dashboard (<https://yourhost:10010/ui/v1/apicatalog/#/dashboard>)
    - No util should be displayed, and API Catalog main page should appear
    - API Mediation Layer API -> All services are running, Sample API Mediation Layer Applications -All services are running tiles should display
    - The `Logout` icon should be displayed 
- Write "Sample" on the search bar
    - API Mediation Layer API tile should disappear, and Sample API Mediation Layer Applications should be there when you stop typing
    - Search Icon should be replaced by clear icon on the right side of search bar
- Press the clear icon ``X`` on the search bar
    - Text on search bar should disappear
    - All tiles should show up again
- Click Sample API Mediation Layer Application
    - No util should be displayed and API Mediation Layer API page(<https://yourhost:10010/ui/v1/apicatalog/#/tile/apicatalog/apicatalog>) should appear
    - Full service title should appear when you hover mouse on top of tab names (discoverableclient, enablerv1samleapp)
- Click on the provided URL </api/v1/apicatalog/apidoc/discoverableclient/v1>
    - A full JSON message should return in a new browser tab, with no HTTP errors
- Click on the second GET request " Get a greeting"
    - Parameters and responses should appear
    - Example Value and Model should toggle 
- Click on the other tab("enablerv1sampleapp")
    - New service "Service Integration Enabler V1 Sample App (spring boot 1.x)" should display
- Press the application "< Back" button
    - should return to catalog dashboard
- Click on API Mediation Layer API tile, verify it is valid, then press the browser back button
    - should return to catalog dashboard
- Click again on a tile, then press the API Catalog header Icon
    - should return to catalog dashboard
- Click on the `Logout` button 
    - you should log out from the API Catalog and be redirected to the login page
- Try to insert some invalid credentials and press the `Sign in` button
    - should display a message saying `Username or password is invalid` and you should not be able to log into the API Catalog
