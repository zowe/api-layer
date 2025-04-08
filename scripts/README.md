scripts
=======

The `scripts` directory contains the following script used during the build process:

`security_sensitive` - node js script to label the PRs as Sensitive

`jacocoagent.jar` and `jacococli.jar` - jacoco tools used in jib images, when java is updated, these must be checked for compatibility. The `runtime` alternative of jacoco agent is required. The current version 0.8.12 does not support Java 25 (next LTS).
