# Contribution Guidelines
This document is a living summary of conventions and best practices for development within Zowe API Mediation Layer.

  - [Understanding Modules](#understanding-modules)
  - [Pull Requests](#pull-requests)
  - [General Guidelines](#general-guidelines)
  - [Code Guidelines](#code-guidelines)
  - [File Naming Guidelines](#file-naming-guidelines)
  - [Testing Guidelines](#testing-guidelines)
  - [Build Process Guidelines](#build-process-guidelines)
  - [Documentation Guidelines](#documentation-guidelines)

## Understanding Modules

**Modules** are individual folders inside ‘api-layer` root folder. Refer to the below table for each module’s purpose.

| Package Folder                           | Purpose                            |
|------------------------------------------|------------------------------------|
| api-catalog-services                     | Core Service - API Catalog         |
| api-catalog-ui                           | Core Service - API Catalog UI      |
| apiml-common                             | Library - Common code with Spring  |
| apiml-security-common                    | Library - Common security code     |
| apiml-utility                            | Library - Utilities                |
| codequality                              | Config - Checkstyle                |
| common-service-core                      | Library - Common code Java         |
| config                                   | Config - Local config              |
| discoverable-client                      | Test Service - Discoverable Client |
| discovery-service                        | Core Service - Discovery Service   |
| docker                                   | Tool - Dev Docker Container        |
| docs                                     | Documentation                      |
| gateway-service                          | Core Service - Gateway Service     |
| integration-enabler-spring-v1-sample-app | Sample Service - Spring Enabler    |
| integration-tests                        | Test - Integration test            |
| keystore                                 | Config - Local TLS config          |
| onboarding-enabler-java                  | APIML SDK - Java Enabler           |
| onboarding-enabler-java-sample-app       | Sample Service - Java Enabler      |
| onboarding-enabler-spring                | APIML SDK - Spring Enabler         |
| passticket                               | Test - Passticket test tools       |
| scripts                                  | Tool - Test and Build scripts      |
| security-service-client-spring           | Library - Security Client          |
| zaas-client                              | APIML SDK - ZAAS Client            |
| zlux-api-catalog                         | Library - Zlux Api Catalog plugin  |
| zowe-install                             | Tool - Zowe run scripts            |

## Pull Requests

Consider the following when you create or respond to pull requests:

- Pull request reviewers should be assigned to a squad team member.
- Use a draft pull request for work in progress that you want to build on the CICD pipeline.
- Anyone can comment on a pull request to request a delay on merging or to get questions answered.
- If you split functionality into several pull requests, consider using a common naming prefix for logical grouping (Github issue number for example).
- Review guidelines for [how to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)
    and [good commits](https://chris.beams.io/posts/git-commit/). 

## General Guidelines

The following list describes general conventions for contributing to api-layer:

- Communicate frequently (before pull request) with team member representatives for new design features.
- Before implementing new functionality, reach out to squad members and discuss architecture and design.
- Get in contact using Slack’s `zowe-api` channel, attend squad meetings or create an `Enhancement` issue on GitHub.
- Reuse logging and error handling patterns already in place.
- Document your code with appropriate Javadoc and create documentation on [docs-site](https://github.com/zowe/docs-site) if needed.
- Provide adequate logging to diagnose problems that happen at external customer sites.
- Use sensible class, method, and variable names.
- Keep core services independent without cross dependencies.
- Keep classes small, maintain Single Responsibility Principle.
- The pull request should include tests.
- Code coverage for new code should be at least 80%. 
- Code coverage should not be lower than on master.
SonarCloud quality gate should be passed and no code smells, security hotspots and bugs should be added.
- If the pull request adds or changes functionality that requires update of packaging or configuration, it needs to be tested on a test system installed from the Zowe PAX file.
- Scripts in [zowe-install](https://github.com/zowe/zowe-install-packaging) directory need to be updated.
- Keep pull requests as small as possible. If a functionality can be split on multiple pull requests which will not break the master after being merged separately, this will speed up the review process and give a better level of safety rather than merging a single pull request containing many lines of code.

## Code Guidelines

Indent code with 4 spaces. This is also documented via `.editorconfig`, which can be used to automatically format the code if you use an [EditorConfig](https://editorconfig.org/) extension for your editor of choice.

Lint rules are enforced through our [build process](#build-process-guidelines).

## File Naming Guidelines

The following list describes the conventions for naming the source files:

Follow Java file, class and package naming conventions
Master package should be `org.zowe.apiml`
Subpackage names are single lowercase words, named by feature. For example `security`,`message`,`config`
Keep the hierarchy shallow

## Testing Guidelines

- Core team uses TDD practices.
- All code in PR should be covered with unit tests.
- Add integration tests where needed. The integration tests are executed on the [Zowe build pipeline](https://wash.zowe.org:8443/job/API_Mediation/), and on our inhouse system as part of the build pipeline. Contact the API Layer squad if you need triage.
- Add UI end to end tests where needed. The end to end tests are executed on [Zowe build pipeline](https://wash.zowe.org:8443/job/API_Mediation/) and on our inhouse system as part of the build pipeline. Contact API Layer squad if you need triage.
- Use meaningful test method names. We use the `given_when_then` pattern.
- Most of our java unit tests are still written in JUnit4, since we didn’t fully migrate them to JUnit5 and we have a backward compatibility package. However, use JUnit5 for new tests.

## Build Process Guidelines

We use `gradle build` task to build a solution. The build executes the following:
Checkstyle lint
License check
Unit tests

You can run [Integration Tests](integration-tests/README.md) locally before creating a pull request.

## Documentation Guidelines

Open a pull request in the [docs-site repository](https://github.com/zowe/docs-site) to create documentation for your contribution.

- Create end-user documentation for how to use your feature, functionality. This end-user documentation can be drafted collaboratively with the tech writer.
- Open an issue in [docs-site repository](https://github.com/zowe/docs-site) if you need assistance.
- End-user documentation requires review and approval by a tech writer. Address all comments raised by the tech writer during review.
- Include a readme.md file within the repository that contains information for developers that, at a minimum, includes an overview, how to build, and how to run tests, if applicable. For example, see [the passticket test programs](https://github.com/zowe/api-layer/blob/master/passticket/test-programs/README.md).

In addition to external documentation, please thoroughly comment your code for future developers who want to understand, use, and enhance your feature.

 ### Javadoc

Methods and classes should have concise javadoc describing usage and purpose.
