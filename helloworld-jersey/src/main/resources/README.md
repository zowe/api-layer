# Sample Java Jersey service

This is a sample Helloword application. 

# How to Run 

After building  deploy the produced .war artifact to a local Tomcat server. 

For Intellij users, go to Run > Edit Configurations, click the + sign on top left and add a new Tomcat server configuration, named Helloword-Jersey. 
Now you can always start your helloword-jersey application by clicking on the Run pannel. 

# How to use

You can see this application registered to your local running catalog under Tile "Sample API Mediation Layer Applications"

To access application directly go to localhost:8080/ui/v1/application(?)

For API requests use endpoints "/greeting" for a generic greet or "greeting/{name}" for a greet returning your input {name}
