allprojects {
    val version= "1.0.2-SNAPSHOT"
    val springBootVersion = "2.1.3.RELEASE"
}

//allprojects {
//    springBootVersion = "2.0.4.RELEASE"
//    springCloudVersion = "2.0.1.RELEASE"
//    springSecurityVersion = "5.1.0.RELEASE"
//
//    slf4jVersion = "1.7.25"
//    jsonPathVersion = "2.4.0"
//    springFoxVersion = "2.8.0"
//    jsonWebTokenVersion = "0.8.0"
//    httpClientVersion = "4.5.3"
//    guavaVersion = "23.2-jre"
//    mockSpringRestVersion = "1.0.2"
//    reactorVersion = "3.0.7.RELEASE"
//    cucumberVersion = "1.2.5"
//    hamcrestVersion = "1.3"
//    javaxServletApiVersion = "3.1.0"
//    jacksonVersion = "2.9.2"
//    restAssuredVersion = "3.0.7"
//    swaggerCoreVersion = "1.5.21"
//    commonsValidatorVersion = "1.6"
//    powerMockVersion = "1.7.3"
//    jacksonCoreVersion = "2.9.6"
//    javaxValidationApiVersion = "2.0.1.Final"
//    esmClientVersion = "2.0.0-SNAPSHOT"
//    gsonVersion = "2.8.2"
//    mockitoCoreVersion = "2.15.0"
//    zsslVersion = "1.0.0"
//    tomcatVersion = "8.5.33"
//    commonsLang3Version = "3.7"
//    gradleGitPropertiesVersion = "1.5.1"
//    jettyWebSocketClientVersion = "9.4.11.v20180605"
//    eurekaClientVersion = "1.8.6"
//    junitVersion = "4.12"
//    logbackVersion = "1.2.3"
//    spring4Version = "4.3.7.RELEASE"
//    awaitilityVersion = "3.0.0"
//    jjwtVersion = "0.9.1"
//    velocityVersion = "2.0"
//    jsoupVersion = "1.8.3"
//    httpCoreVersion = "4.4.10"
//    snakeyamlVersion = "1.23"
//    springHateoasVersion = "0.23.0.RELEASE"
//    springRetryVersion = "1.2.2.RELEASE"
//    jsonUnitVersion = "1.25.0"
//    swaggerJerseyJaxrsVersion = "1.5.10"
//    jacksonDataformatYamlVersion = "2.9.7"
//    jerseyVersion = "2.26"
//
//    val libraries = [
//        "slf4j_simple" : "org.slf4j:slf4j-simple:$slf4jVersion",
//        "slf4j_api" : "org.slf4j:slf4j-api:$slf4jVersion"
//    ],
////        spring_boot_gradle_plugin          : "org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}",
////        spring_boot_starter_actuator       : "org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}",
////        spring_boot_starter_security       : "org.springframework.boot:spring-boot-starter-security:${springBootVersion}",
////        spring_boot_starter_web            : "org.springframework.boot:spring-boot-starter-web:${springBootVersion}",
////        spring_boot_starter_websocket      : "org.springframework.boot:spring-boot-starter-websocket:${springBootVersion}",
////        spring_boot_starter_test           : "org.springframework.boot:spring-boot-starter-test:${springBootVersion}",
////        spring_boot_starter_mobile         : "org.springframework.boot:spring-boot-starter-mobile:${springBootVersion}",
////        spring_boot_starter_web_services   : "org.springframework.boot:spring-boot-starter-web-services:${springBootVersion}",
////        spring_boot_configuration_processor: "org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}",
////        spring_boot_starter_webflux        : "org.springframework.boot:spring-boot-starter-webflux:${springBootVersion}",
////        spring_boot_starter_aop            : "org.springframework.boot:spring-boot-starter-aop:${springBootVersion}",
////        spring_boot_starter_log4j2         : "org.springframework.boot:spring-boot-starter-log4j2:${springBootVersion}",
////        spring_boot_starter_thymeleaf      : "org.springframework.boot:spring-boot-starter-thymeleaf:${springBootVersion}",
////        spring_boot_devtools               : "org.springframework.boot:spring-boot-devtools:${springBootVersion}",
////        spring_retry                       : "org.springframework.retry:spring-retry:${springRetryVersion}",
////        spring_hateoas                     : "org.springframework.hateoas:spring-hateoas:${springHateoasVersion}",
////
////        spring_cloud_config_server         : "org.springframework.cloud:spring-cloud-config-server:${springCloudVersion}",
////        spring_cloud_starter_config        : "org.springframework.cloud:spring-cloud-starter-config:${springCloudVersion}",
////        spring_cloud_starter_zuul          : "org.springframework.cloud:spring-cloud-starter-netflix-zuul:${springCloudVersion}",
////        spring_cloud_starter_eureka        : "org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:${springCloudVersion}",
////        spring_cloud_starter_eureka_server : "org.springframework.cloud:spring-cloud-starter-netflix-eureka-server:${springCloudVersion}",
////        spring_cloud_starter_ribbon        : "org.springframework.cloud:spring-cloud-starter-netflix-ribbon:${springCloudVersion}",
////
////        spring_security_web                : "org.springframework.security:spring-security-web:${springSecurityVersion}",
////        spring_security_config             : "org.springframework.security:spring-security-config:${springSecurityVersion}",
////        spring_security_test               : "org.springframework.security:spring-security-test:${springSecurityVersion}",
////
////        jsoup                              : "org.jsoup:jsoup:${jsoupVersion}",
////        json_path                          : "com.jayway.jsonpath:json-path:${jsonPathVersion}",
////        springFox                          : "io.springfox:springfox-swagger2:${springFoxVersion}",
////        springFoxSwaggerUI                 : "io.springfox:springfox-swagger-ui:${springFoxVersion}",
////        springFoxWeb                       : "io.springfox:springfox-spring-web:${springFoxVersion}",
////        swagger_core                       : "io.swagger:swagger-core:${swaggerCoreVersion}",
////        swagger_models                     : "io.swagger:swagger-models:${swaggerCoreVersion}",
////        swagger_annotations                : "io.swagger:swagger-annotations:${swaggerCoreVersion}",
////        http_client                        : "org.apache.httpcomponents:httpclient:${httpClientVersion}",
////        http_core                          : "org.apache.httpcomponents:httpcore:${httpCoreVersion}",
////
////        mockSpringRest                     : "com.github.skjolber:mockito-rest-spring:${mockSpringRestVersion}",
////        reactorCore                        : "io.projectreactor:reactor-core:${reactorVersion}",
////        reactorTestSupport                 : "io.projectreactor.addons:reactor-test:${reactorVersion}",
////        hamcrest                           : "org.hamcrest:hamcrest-all:${hamcrestVersion}",
////        javax_servlet_api                  : "javax.servlet:javax.servlet-api:${javaxServletApiVersion}",
////        rest_assured                       : "io.rest-assured:rest-assured:${restAssuredVersion}",
////        spring_mock_mvc                    : "io.rest-assured:spring-mock-mvc:${restAssuredVersion}",
////        commons_validator                  : "commons-validator:commons-validator:${commonsValidatorVersion}",
////        powermock_api_mockito2             : "org.powermock:powermock-api-mockito2:${powerMockVersion}",
////        power_mock_junit4                  : "org.powermock:powermock-module-junit4:${powerMockVersion}",
////        power_mock_junit4_rule             : "org.powermock:powermock-module-junit4-rule:${powerMockVersion}",
////        jackson_core                       : "com.fasterxml.jackson.core:jackson-core:${jacksonCoreVersion}",
////        jackson_databind                   : "com.fasterxml.jackson.core:jackson-databind:${jacksonCoreVersion}",
////        jackson_annotations                : "com.fasterxml.jackson.core:jackson-annotations:${jacksonCoreVersion}",
////        jackson_dataformat_yaml            : "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonCoreVersion}",
////        esm_client                         : "dbm:esm-client:${esmClientVersion}",
////        javax_validation                   : "javax.validation:validation-api:${javaxValidationApiVersion}",
////        gson                               : "com.google.code.gson:gson:${gsonVersion}",
////        mockito_core                       : "org.mockito:mockito-core:${mockitoCoreVersion}",
////        tomcat_coyote                      : "org.apache.tomcat:tomcat-coyote:${tomcatVersion}",
////        tomcat_embed_core                  : "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}",
////        apacheCommons                      : "org.apache.commons:commons-lang3:${commonsLang3Version}",
////        jetty_websocket_client             : "org.eclipse.jetty.websocket:websocket-client:${jettyWebSocketClientVersion}",
////        eureka_client                      : "com.netflix.eureka:eureka-client:${eurekaClientVersion}",
////        junit                              : "junit:junit:${junitVersion}",
////        logback_classic                    : "ch.qos.logback:logback-classic:${logbackVersion}",
////        spring4Mvc                         : "org.springframework:spring-webmvc:${spring4Version}",
////        spring4Test                        : "org.springframework:spring-test:${spring4Version}",
////        awaitility                         : "org.awaitility:awaitility:${awaitilityVersion}",
////        jjwt                               : "io.jsonwebtoken:jjwt:${jjwtVersion}",
////        apache_velocity                    : "org.apache.velocity:velocity-engine-core:${velocityVersion}",
////        snakeyaml                          : "org.yaml:snakeyaml:${snakeyamlVersion}",
////        json_unit                          : "net.javacrumbs.json-unit:json-unit:${jsonUnitVersion}",
////        json_unit_fluent                   : "net.javacrumbs.json-unit:json-unit-fluent:${jsonUnitVersion}",
////        swagger_jersey2_jaxrs              : "io.swagger:swagger-jersey2-jaxrs:${swaggerJerseyJaxrsVersion}",
////        jackson_dataformat_yaml            : "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonDataformatYamlVersion}",
////        jersey_server                      : "org.glassfish.jersey.core:jersey-server:${jerseyVersion}",
////        jersey_hk2                         : "org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}",
////        jersey_container_servlet_core      : "org.glassfish.jersey.containers:jersey-container-servlet-core:${jerseyVersion}",
////        jersey_media_json_jackson          : "org.glassfish.jersey.media:jersey-media-json-jackson:${jerseyVersion}",
////        jersey_test_provider_jdk_http      : "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-jdk-http:${jerseyVersion}"
//
//}
