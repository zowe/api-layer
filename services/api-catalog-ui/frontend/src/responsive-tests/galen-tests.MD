## Galen tests for responsiveness

[Galen](http://galenframework.com/) is a test framework which allows to test the responsiveness of UI.

#### How to write test

Follow the steps below in order to test components:
1.  Inside your _*.test.jsx_ create a driver so that you can run an instance of the browser (by default Firefox is used as browser, but you can use a different browser like Chrome by modifying the [galen.config](../../galen.config) ). You need to specify the URL of your web page that you want to test, and optionally you can also set resolution, so you can  test your UI on different simulated devices (tablet, desktop, mobile...)
2.  Create a _*.gspec_ file. This is a configuration file where you will define all the elements that you want to check along with the rules (for example visibility, distance between elements, width etc.)
3. (Optional) In case you want to perform functional testing and you need to interact with elements on page it is best to use GalenPages. It is a small lightweight framework for working with page object model (check the _pages_ directory)

**Example:**

Below is an example of how to define rules in the _*.gspec_ file:

   ```
         @on tablet
                tile2:
                    aligned vertically all tile4
            @on desktop, mobile, tablet
                tile2:
                    visible
            @on desktop
                tile2:
                    aligned horizontally all tile3
            @on mobile
                tile2:
                    aligned vertically all tile3
        
        
            @on tablet
                tile3:
                    aligned horizontally all tile4
                    aligned vertically all tile5
            @on desktop, mobile, tablet
                tile3:
                    visible
            @on desktop
                tile3:
                    aligned horizontally all tile4
            @on mobile
                tile3:
                    aligned vertically all tile4
   ```
   
#### How to run tests

1.  Start the instance to test

    ```yaml
    npm run start
    ```
2. Run Gateway service, Discovery service, API Catalog service and Discoverable Client Sample service.
3.  From the root of the project, run 
    ```
    npm run test:galen
    ``` 
to run all the tests created inside the _tests_ folder. You will also find reports within the _report_ folder.


