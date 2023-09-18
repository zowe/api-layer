# Contribution Guidelines
This document is a living summary of conventions and best practices for development within Zowe API Mediation Layer.

  - [Contact Us](#contact-us)
  - [Understanding Modules](#understanding-modules)
  - [Pull Requests](#pull-requests)
  - [General Guidelines](#general-guidelines)
  - [Code Guidelines](#code-guidelines)
  - [File Naming Guidelines](#file-naming-guidelines)
  - [Branch Naming Guidelines](#branch-naming-guidelines)
  - [Commit Message Structure Guideline](#commit-message-structure-guideline)
  - [Testing Guidelines](#testing-guidelines)
  - [Build Process Guidelines](#build-process-guidelines)
  - [Documentation Guidelines](#documentation-guidelines)
  - [Planning Guidelines](#planning-guidelines)
  - [Retrospective Guidelines](#retrospective-guidelines)

## Contact Us

Get in touch using [Zowe Communication Channels](https://github.com/zowe/community/blob/master/README.md#communication-channels). You can find us in the `#zowe-api` channel on Slack.

## Understanding Modules

**Modules** are individual folders inside ‘api-layer` root folder. Refer to the below table for the purpose of the more important modules.

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
| gradle                                   | Config - Project-wide Gradle tasks |
| integration-tests                        | Test - Integration test            |
| keystore                                 | Config - Local TLS config          |
| metrics-service                          | Core Service - Metrics Service     |
| metrics-service-ui                       | Core - Service - Metrics Service UI|
| mock-services                            | Test - Mock z/OS services          |
| onboarding-enabler-java                  | APIML SDK - Java Enabler           |
| onboarding-enabler-java-sample-app       | Sample Service - Java Enabler      |
| onboarding-enabler-micronaut             | APIML SDK - Micronaut Enabler      |
| onboarding-enabler-micronaut-sample-app  | Sample Service - Micronaut Enabler |
| onboarding-enabler-nodejs                | APIML SDK - Node.js Enabler        |
| onboarding-enabler-nodejs-sample-app     | Sample Service - Node.js Enabler   |
| onboarding-enabler-spring                | APIML SDK - Spring Enabler         |
| onboarding-enabler-spring-sample-app     | Sample Service - Spring Enabler    |
| passticket                               | Test - Passticket test tools       |
| scripts                                  | Tool - Test and Build scripts      |
| security-service-client-spring           | Library - Security Client          |
| zaas-client                              | APIML SDK - ZAAS Client            |
| zlux-api-catalog                         | Library - Zlux Api Catalog plugin  |

## General Guidelines

The following list describes general conventions for contributing to api-layer:

- Communicate frequently (before pull request) with team member representatives for new design features.
- Before implementing new functionality, reach out to squad members and discuss architecture and design.
- Get in contact using Slack’s `#zowe-api` channel, attend squad meetings or create an `Enhancement` issue on GitHub.
- Reuse logging and error handling patterns already in place.
- Document your code with appropriate Javadoc and inline comments. In JavaScript parts of the code please use JSDoc style comments. 
- Create end-user documentation on [docs-site](https://github.com/zowe/docs-site) if needed.
- Provide adequate logging to diagnose problems that happen at external customer sites.
- Use sensible class, method, and variable names.
- Keep core services independent without cross dependencies.
- Keep classes small, maintain Single Responsibility Principle.
- Pull requests should include tests.
- Code coverage for new code should be at least 80%.
- Code coverage should not be lower than on master.
- SonarCloud quality gate should be passed and no code smells, security hotspots and bugs should be added in pull requests.
- If the pull request adds or changes functionality that requires an update of packaging or configuration, it needs to be tested on a test system installed from the Zowe PAX file.
- Keep pull requests as small as possible. If functionality can be split into multiple pull requests which will not break the master after being merged separately, this will speed up the review process and give a better level of safety rather than merging a single pull request containing many lines of code.

## Branch Naming Guidelines

There are two ways to name new branches to simplify orientation. One is used for work on Github issues and one is used for small contributions that don't deserve an issue.

GitHub Issues: `<team-tag>/<work-tag>/<name-tag>` an example would be: `rip/GH752/per_service_timeout_options`

Small personal contributions: `private/<person-tag>/<name-tag>` an example would be: `private/jb892003/temporarily_disable_e2e_tests`

- team-tag

The team contributing on the Broadcom side for example is names Rest In aPi and so the `rip` is used. If there isn't a team involved, use your personal Github handle e.g. `balhar-jakub` or `jandadav`

- work-tag

Represents a codified and searchable reference to problem that the branch solves. For Github issues, you would use `GH` prefix and `Github issue number`.

- person-tag

Represents a unique identifier for specific person. The good candidate is the Github handle such as `balhar-jakub` or `jandadav`.

- name-tag

Please keep the name short and relevant.

## File Naming Guidelines

The following list describes the conventions for naming the source files:

- Follow Java file, class and package naming conventions.
- Master package should be `org.zowe.apiml`.
- Subpackage names are single lowercase words, named by feature. For example `security`,`message`,`config`.
- Keep the hierarchy shallow.

## Code Guidelines

Indent code with 4 spaces. This is also documented via `.editorconfig`, which can be used to automatically format the code if you use an [EditorConfig](https://editorconfig.org/) extension for your editor of choice.

Lint rules are enforced through our [build process](#build-process-guidelines).

## Testing Guidelines

- Core team uses TDD practices.
- All code in PRs should be covered with unit tests.
- Add integration tests where needed. The integration tests are executed with [Github Actions](https://github.com/zowe/api-layer/actions) using the workflows defined in [.github/workflows](.github/workflows). Contact the API Layer squad if you need triage. The Mock zOSMF service is used for verifying integration with zOSMF.
- Add UI end to end tests where needed. The end to end tests are executed with [Github Actions](https://github.com/zowe/api-layer/actions) using the workflows defined in [.github/workflows](.github/workflows). Contact API Layer squad if you need triage.
- Use meaningful test method names. We use the `given_when_then` pattern.
- Leverage `@Nested` annotation for test method grouping where possible. It makes the tests more organized and readable. The test method names are generally shorter.
- When adding tests to method not following `given_when_then` or not leveraging the `@Nested` annotation refactor the class before adding further tests.
- Example of well written test: [CachingControllerTest.java](https://github.com/zowe/api-layer/blob/master/caching-service/src/test/java/org/zowe/apiml/caching/api/CachingControllerTest.java). It uses `@Nested` annotation to separate the test scenarios into groups, which are individually setup. The tests are short and clear and the method names clearly convey what is tested.
- Some of our java unit tests are still written in JUnit4, since we didn’t fully migrate them to JUnit5 and we have a backward compatibility package. However, use JUnit5 for new tests.

## Build Process Guidelines

We use `gradle build` task to build a solution. The build executes the following:
- Checkstyle lint
- License check
- Unit tests

You can run [Integration Tests](integration-tests/README.md) locally before creating a pull request.

## Commit Message Structure Guideline

Commits going to a master branch should stick to the Conventional Commits specification. This is a lightweight convention on the top of the commit messages. 
Template:
```
<type>[optional scope]: <description>

[optional body]

[footer(s)]
```
Basic example:
```
feat(authentication): Introducing x509 as a form of authentication

This is a body, which is purely optional. One can use this section if description is not enough to provide insight. 
Can also contains notes and hints. Should not be too long.

Signed-off-by: John Doe <john.doe@zowe.org>
```

### Type
 - fix: patches a bug in your codebase (this correlates with PATCH in semantic versioning)
 - feat: introduces a new feature to the codebase (this correlates with MINOR in semantic versioning)
 - docs: affecting the documentation 
 - refactor: refactoring the code
 - chore: cleaning in general, update dependencies

Type or scope appended with `!` has the same meaning as BREAKING CHANGE(explained in footer section). It introduces a breaking API change (correlating with MAJOR in semantic versioning). MUST be used with caution!

### Scope
Optional part of the message. Identifies a part of the codebase altered byt this commit. Examples could be: authentication, Discovery service, ...

### Description
A description MUST immediately follow the colon and space after the type/scope prefix. The description is a short summary of the code changes, e.g., `fix: array parsing issue when multiple spaces were contained in string`.

### Body
A commit body is free-form and MAY consist of any number of newline separated paragraphs.

### Footer
 - Signed-off-by: every commit needs to be signed by at least one author 
 - Reviewed-by: (OPTIONAL) is a plus, but not necessary
 - Co-authored-by: (OPTIONAL) in case of more contributors engaged 
 - BREAKING CHANGE: (OPTIONAL) introduces a breaking API change (correlating with MAJOR in semantic versioning). A BREAKING CHANGE can be part of commits of any type. MUST be used with caution!

## Pull Requests

Consider the following when you create or respond to pull requests:

- Every pull request must have associated issue in [api-layer repository](https://github.com/zowe/api-layer/issues/) and link to it
- Pull request reviewers should be assigned to a squad team member.
- Use a draft pull request for work in progress that you want to build on the CICD pipeline.
- Anyone can comment on a pull request to request a delay on merging or to get questions answered.
- If you split functionality into several pull requests, consider using a common naming prefix for logical grouping (Github issue number for example).
- Review guidelines for [how to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)
  and [good commits](https://chris.beams.io/posts/git-commit/).

## API ML repository automated builds using GH actions
Automated builds of api-layer repo are run as workflows. They are defined in the folder .github/workflows
General info about workflows https://docs.github.com/en/actions/using-workflows/about-workflows
Reusable workflows are stored either in local repo .github/actions or https://github.com/zowe-actions/shared-actions e.g. for publishing the images https://github.com/zowe-actions/shared-actions/tree/main/prepare-workflow

Usage of workflows:
- release https://github.com/zowe/api-layer/blob/v2.x.x/docs/release.md#github-actions
- build + unit tests + test of correct registration of all services: service-registration.yml
- generate changelog after release: changelog.yml
- update docs with new error messages: docs.yml
- all flavors of integration tests: integration-tests.yml
- publish identify federation plugin: zowe-cli-deploy-component.yml

Services are using JIB containers which are built by PublishJibContainers and all other jobs that are using JIBs must depend on this, so they have the latest build
JIB definition is stored in gradle/jib.gradle
- docs https://github.com/GoogleContainerTools/jib/blob/master/jib-maven-plugin/README.md
- directories whose content is copied into images: ['../config', '../keystore', '../scripts']
- additional config location is set to `/docker` and config location is therefore stored in `<root>/config/docker`
  How to start new component
- define a new record in `services`
- the name of the component will become its hostname
- select image
- change configuration using environment variable in `env`


## Security fixes

To provide long-term support(LTS) for versions in maintenance mode, any security fix must be merged to the master branch as a separate commit. The reasoning behind these requirements is, that security fixes will be cherry-picked to the maintenance versions of API Mediation layer.

## Documentation Guidelines

Open a pull request in the [docs-site repository](https://github.com/zowe/docs-site) to create documentation for your contribution.

- Create end-user documentation for how to use your feature, functionality. This end-user documentation can be drafted collaboratively with a tech writer.
- Open an issue in [docs-site repository](https://github.com/zowe/docs-site) if you need assistance.
- End-user documentation requires review and approval by a tech writer. Address all comments raised by the tech writer during review.
- Include a readme.md file within the repository that contains information for developers that, at a minimum, includes an overview, how to build, and how to run tests, if applicable. For example, see [the passticket test programs](https://github.com/zowe/api-layer/blob/master/passticket/test-programs/README.md).

In addition to external documentation, please thoroughly comment your code for future developers who want to understand, use, and enhance your feature.

 ### Javadoc

Methods and classes should have concise javadoc describing usage and purpose.

## Planning Guidelines

The new issues raised in the GitHub are triaged and sized weekly in the Wednesday Squad meetings. There is an [Open Mainframe Project
Zowe calendar](https://lists.openmainframeproject.org/calendar) with the squad meetings.

In order to get a better understanding for the complexity of different issues and to improve the quality and reliability of
our planning we size issues that the squad takes responsibility for. The sizing is relative considering the complexity of a particular issue compared to the others.

For sizing we use the Fibonacci sequence: 1,2,3,5,8,13 The higher the number the more complex the work involved or more uncertainty
around how to implement the solution.  

### Examples of given size

-   0.5 The smallest meaningful issue that delivers value on its own. An example: [Explore CodeQL for for Github Actions](https://github.com/zowe/api-layer/issues/1263)
-   1 Usually no collaboration within the squad is necessary and the fix can be delivered mainly by one team member. An example: [Streamline Single Sign On Documentation](https://github.com/zowe/api-layer/issues/677) 
-   2 Ideal size of a story. It allows meaningful collaboration (i.e. to split the issue into separate tasks among multiple members). The issue is delivered within a Sprint. An example: [Add logout functionality to the ZAAS client](https://github.com/zowe/api-layer/issues/808) 
-   3 Good size of a story. It is possible to collaborate among multiple members. The fix to the issue is usually delivered within a Sprint. An example: [Alpha of client certificate using SAF API](https://github.com/zowe/api-layer/issues/758)
-   5 Problems start at this size. If possible split the issue into multiple smaller ones and focus namely on the value delivered (i.e. do the smaller issues still bring value if delivered separately?). Unless it is possible to collaborate on the issue among more members of the squad, it is possible that the issue won't fit into one sprint. An example: [Support the x509 client certificate authentication via x509](https://github.com/zowe/api-layer/issues/827)
-   8 Large issue. Unless there is a good way to split the issue or collaborate, the risk of not being able to deliver the issue within a sprint is quite large. Split to smaller issues if possible. An example: [Implement Caching Service](https://github.com/zowe/api-layer/issues/863)
-   13 WARNING: Here be lions! We haven't used this size so far. If it occurs split the issue into smaller issues. 

## Retrospective Guidelines

The squad regularly observes the following metrics and actively explores areas for continuous improvement

-  [Cummulative flow report](https://app.zenhub.com/workspaces/community-5c93e02fa70b456d35b8f0ed/reports/cumulative) for the work in progress
-  [Control chart report](https://app.zenhub.com/workspaces/community-5c93e02fa70b456d35b8f0ed/reports/control?df=08-17-2020&dr=24m&dt=08-17-2022&ep=Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzA&labels=&notLabels[]=mentee&notLabels[]=exclude&prs=false&r[]=145870120&r[]=162463317&r[]=143049506&r[]=144619729&r[]=152270012&r[]=300586575&r[]=150100207&r[]=176523400&r[]=141316148&r[]=144599701&r[]=144600062&r[]=151615191&r[]=151624320&r[]=168378275&r[]=144592776&r[]=144595426&r[]=166097436&r[]=355928090&r[]=157852345&r[]=481718102&r[]=158274751&r[]=151619852&r[]=184306542&r[]=177186770&r[]=423588810&r[]=234608353&r[]=197354800&s=day&sp=Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzE5NDEyMTY) for cycle time
-  [Opened pull requests](https://github.com/zowe/api-layer/pulls)
-  [Binary snapshot release github action](https://github.com/zowe/api-layer/actions/workflows/binary-snapshot-release.yml?query=branch%3Av2.x.x) for stability
-  [GitHub Insights](https://github.com/zowe/api-layer/pulse)
-  [Linux Foundation Insights](https://insights.lfx.linuxfoundation.org/projects/open-mainframe-project%2Fzowe/dashboard;subTab=technical;v=pull-request-management%2Fgithub-pr%2Ftiming?filter=%23%2Fdashboard%2FGitHub-Pull-Requests-Timing%3Fembed%3Dtrue%26_g%3D(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:%27now-90d%27,to:%27now%27))%26_a%3D(description:%27GitHub%2520Pull%2520Requests%2520Timing%2520panel%27,filters:!((%27$state%27:(store:appState),meta:(alias:!n,disabled:!f,index:github_issues,key:author_bot,negate:!t,params:(query:!t),type:phrase),query:(match:(author_bot:(query:!t,type:phrase)))),(%27$state%27:(store:appState),meta:(alias:!n,controlledBy:%271620306076770%27,disabled:!f,index:github_issues,key:repo_short_name,negate:!f,params:(query:api-layer),type:phrase),query:(match_phrase:(repo_short_name:api-layer)))),fullScreenMode:!f,options:(darkTheme:!f,useMargins:!t),panels:!((embeddableConfig:(title:Filter),gridData:(h:15,i:e09a40b0-b594-40cc-94ae-c6a1798aee4f,w:12,x:0,y:0),id:c7ba3dc0-a511-11ea-83d0-e156a256d6e6,panelIndex:e09a40b0-b594-40cc-94ae-c6a1798aee4f,title:Filter,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:Summary),gridData:(h:7,i:ff386d90-97fa-4927-8a4f-7d27962b8cbc,w:17,x:12,y:0),id:%279b029f40-a798-11eb-9005-3930817a030d%27,panelIndex:ff386d90-97fa-4927-8a4f-7d27962b8cbc,title:Summary,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27Pull%2520Requests%2520by%2520Organization%27),gridData:(h:12,i:%273b37be30-e415-4a40-b908-277f8180803a%27,w:19,x:29,y:0),id:%2730b36640-a9cf-11eb-bd79-f3db3ff39743%27,panelIndex:%273b37be30-e415-4a40-b908-277f8180803a%27,title:%27Pull%2520Requests%2520by%2520Organization%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Summary%27),gridData:(h:8,i:cbc32322-d024-4c0f-9f5f-f47aff2f2180,w:17,x:12,y:7),id:%27634bd8e0-ad0c-11eb-b42d-29bb7e46b0a1%27,panelIndex:cbc32322-d024-4c0f-9f5f-f47aff2f2180,title:%27About%2520Summary%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Pull%2520Requests%2520by%2520Organization%27),gridData:(h:3,i:%27614ffa22-711f-45eb-a0f9-086941b0f16d%27,w:19,x:29,y:12),id:%279801d740-ae7b-11eb-bd79-f3db3ff39743%27,panelIndex:%27614ffa22-711f-45eb-a0f9-086941b0f16d%27,title:%27About%2520Pull%2520Requests%2520by%2520Organization%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27Median%2520Time%2520To%2520Merge%2520(Days)%27),gridData:(h:18,i:b465b3df-c951-4886-8477-7f8b2344b9c2,w:25,x:0,y:15),id:ebbfbea0-8b18-11eb-af30-0df96ab653b0,panelIndex:b465b3df-c951-4886-8477-7f8b2344b9c2,title:%27Median%2520Time%2520To%2520Merge%2520(Days)%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:Submitters),gridData:(h:18,i:e8bc53b3-a848-421a-bcf2-2756f8856787,w:23,x:25,y:15),id:%275413c5b0-8b18-11eb-af30-0df96ab653b0%27,panelIndex:e8bc53b3-a848-421a-bcf2-2756f8856787,title:Submitters,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Median%2520Time%2520To%2520Merge%2520(Days)%27),gridData:(h:4,i:fa7b658d-5ab4-4441-bd84-a2888f301c3a,w:25,x:0,y:33),id:e8c80620-ae7d-11eb-bd79-f3db3ff39743,panelIndex:fa7b658d-5ab4-4441-bd84-a2888f301c3a,title:%27About%2520Median%2520Time%2520To%2520Merge%2520(Days)%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Submitters%27),gridData:(h:4,i:%2702d7f1c8-e4b5-4d58-8ea9-f90c2299d54b%27,w:23,x:25,y:33),id:%273cb25530-ae7c-11eb-bd79-f3db3ff39743%27,panelIndex:%2702d7f1c8-e4b5-4d58-8ea9-f90c2299d54b%27,title:%27About%2520Submitters%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27Median%2520Time%2520To%2520First%2520Review%27),gridData:(h:15,i:dfa0b229-bd78-4f81-837e-41bd5daca192,w:25,x:0,y:37),id:ad25de90-9d65-11eb-94e8-4323c8335d1a,panelIndex:dfa0b229-bd78-4f81-837e-41bd5daca192,title:%27Median%2520Time%2520To%2520First%2520Review%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27Median%2520Time%2520To%2520First%2520Approval%27),gridData:(h:15,i:db1b8b35-f2eb-4e98-91a9-9a92dcc5d5d0,w:23,x:25,y:37),id:%27839da990-a36e-11eb-94e8-4323c8335d1a%27,panelIndex:db1b8b35-f2eb-4e98-91a9-9a92dcc5d5d0,title:%27Median%2520Time%2520To%2520First%2520Approval%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Median%2520Time%2520To%2520First%2520Review%27),gridData:(h:3,i:%2772038382-8d4d-4793-81b4-6783e97388bd%27,w:25,x:0,y:52),id:%271dea0e10-ae7f-11eb-bd79-f3db3ff39743%27,panelIndex:%2772038382-8d4d-4793-81b4-6783e97388bd%27,title:%27About%2520Median%2520Time%2520To%2520First%2520Review%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:%27About%2520Median%2520Time%2520To%2520First%2520Approval%27),gridData:(h:3,i:c2074057-37fe-4699-8bd1-1bc983e960bd,w:23,x:25,y:52),id:%279e381fd0-ae7f-11eb-bd79-f3db3ff39743%27,panelIndex:c2074057-37fe-4699-8bd1-1bc983e960bd,title:%27About%2520Median%2520Time%2520To%2520First%2520Approval%27,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:Submitters),gridData:(h:19,i:d033f04c-1daf-4817-bf2c-f769253af0a8,w:25,x:0,y:55),id:%27045af160-a9d1-11eb-bd79-f3db3ff39743%27,panelIndex:d033f04c-1daf-4817-bf2c-f769253af0a8,title:Submitters,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:Organizations),gridData:(h:19,i:b8ac71fe-9909-4e15-812e-869d24fcff83,w:23,x:25,y:55),id:%274e6ec280-a9d2-11eb-bd79-f3db3ff39743%27,panelIndex:b8ac71fe-9909-4e15-812e-869d24fcff83,title:Organizations,type:visualization,version:%277.6.2%27),(embeddableConfig:(title:Repositories),gridData:(h:19,i:a73c24f5-c180-4a0e-8de7-ab8dd6ebf450,w:48,x:0,y:74),id:%275d309270-a9d3-11eb-bd79-f3db3ff39743%27,panelIndex:a73c24f5-c180-4a0e-8de7-ab8dd6ebf450,title:Repositories,type:visualization,version:%277.6.2%27)),query:(language:lucene,query:%27*%27),timeRestore:!f,title:%27GitHub%2520Pull%2520Requests%2520Timing%27,viewMode:view)&time=%7B%22from%22:%22now-90d%22,%22type%22:%22datemath%22,%22to%22:%22now%22%7D) on GitHub PR Timing
-  [sonarcloud quality gate](https://sonarcloud.io/summary/new_code?id=zowe_api-layer)
-  [Squad's velocity sheet](https://ibm.box.com/s/i8tjcyxxhqlcd6u8dbtqw8p1vlp9izsi)
-  [Squad's Trello retrospective board](https://trello.com/b/EHud02kR/zowe-api-ml-squad-retrospectives)
-  on a biweekly basis we discuss on the Monday calls what we [managed to achieve in the past iteration](https://app.zenhub.com/workspaces/community-5c93e02fa70b456d35b8f0ed/reports/control?df=11-21-2022&dr=14d&dt=12-05-2022&ep=Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzA&labels=&notLabels[]=mentee&notLabels[]=exclude&prs=false&r[]=145870120&r[]=162463317&r[]=143049506&r[]=144619729&r[]=152270012&r[]=300586575&r[]=150100207&r[]=176523400&r[]=141316148&r[]=144599701&r[]=144600062&r[]=151615191&r[]=151624320&r[]=168378275&r[]=144592776&r[]=144595426&r[]=166097436&r[]=355928090&r[]=157852345&r[]=481718102&r[]=158274751&r[]=151619852&r[]=184306542&r[]=177186770&r[]=423588810&r[]=234608353&r[]=197354800&s=day&sp=Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzE5NDEyMTY) and learn from that, pay attention to the actual [start and end dates of a particular iteration](https://github.com/zowe/community/blob/master/Project%20Management/Schedule/Zowe%20PI%20%26%20Sprint%20Cadence.md)
