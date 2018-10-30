# IntelliJ Idea setup

If your editor of choice happens to be Idea and you wnat to use its 'Run Dashboard' follow these steps:

First of all enable _Annotations processing_ if you haven't done so already (Just go to settings and search for 'annotation').

1. Go to 'Run Dashboard'

2. Right click a service and select 'Edit Configuration' (or press F4 while the service is selected)

3. Clear all 'VM options' in the 'Environment' section

4. Then under the 'Override parameters' section add a new parameter `--spring.config.additional-location` and its value `file:./config/local/{SERVICE_NAME}.yml`

5. Run the service
