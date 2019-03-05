rootProject.name = "api-mediation-layer"

/// Core Services
//include(":discovery-service")
//include(":gateway-service")
//include(":discoverable-client")
//include(":security-service")
//
///// Core libraries
//include(":core-library")
//include(":security-module")
//include(":gateway-common")
//include(":enabler-spring-boot-v2")
//include(":enabler-spring-boot-v1")

// New Structure

//Libraries
//include("libraries:apiml-core", "libraries:gateway-common","libraries:enabler-java", "libraries:enabler-spring-boot-v2", "libraries:enabler-spribg-boot-v1")
include("libraries:apiml-core")
include("libraries:security-library")
include("libraries:gateway-common")
//include("libraries:enabler-java")
//include("libraries:enabler-spring-boot-v2")
//include("libraries:enabler-spribg-boot-v1")


// Core Services
//include( "services:discovery", "services:gateway", "services:api-catalog")

// Sample Applications
//include("sample-apps:sample-app-enables-v1")
//include("sample-apps:sample-app-enables-v2")
//include("sample-apps:sample-app-expressjs")
//include("sample-apps:sample-app-jersey")
//include("sample-apps:sample-app-spring")

/// Tests
//include(":message-tests")

//include 'helloworld-jersey'
//include 'message-enabler-spring-v1'
//include 'message-enabler-spring-v1-sample-app'
//include 'zowe-install'
//include 'helloworld-spring'
//include 'message-enabler-java'
//include 'api-catalog-services'
//include 'api-catalog-ui'


