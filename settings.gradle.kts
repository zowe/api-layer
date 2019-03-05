rootProject.name = "api-mediation-layer"

// Core Libraries
include(":libraries:apiml-core")
include(":libraries:security-library")
include(":libraries:gateway-common")
include(":libraries:enabler-java")
//include("libraries:enabler-spring-boot-v2")
//include("libraries:enabler-spribg-boot-v1")


// Core Services
include(":services:discovery")
include(":services:gateway")
//include( "services:discovery", "services:gateway", "services:api-catalog")

// Sample Applications
//include("sample-apps:sample-app-enables-v1")
//include("sample-apps:sample-app-enables-v2")
//include("sample-apps:sample-app-expressjs")
//include("sample-apps:sample-app-jersey")
//include("sample-apps:sample-app-spring")

// Tests
//include(":message-tests")

// Other
//include 'zowe-install'
//include 'api-catalog-ui'


