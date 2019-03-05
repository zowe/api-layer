plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libraries:apiml-core"))
    implementation(project(":libraries:security-library"))
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.3.RELEASE")
    implementation("com.netflix.eureka:eureka-client:1.9.9")
    implementation("org.apache.httpcomponents:httpclient:4.5.7")
    implementation("org.eclipse.jetty:jetty-server:9.4.15.v20190215")



    testImplementation("junit:junit:4.12")
}


//dependencies {

//    compile(libraries.spring_boot_starter_web)
//    compile(libraries.commons_validator)
//    compile(libraries.spring_cloud_starter_eureka_server)
//    compile(libraries.jackson_databind)
//    compile(libraries.apacheCommons)
//    compile(libraries.http_client)
//    compile(libraries.http_core)
//    compile(libraries.jetty_websocket_client)
//    
//    compileOnly(libraries.spring_boot_configuration_processor)
//    
//    testCompile(libraries.javax_servlet_api)
//    testCompile(libraries.spring_boot_starter_test)
//    testCompile(libraries.powermock_api_mockito2)
//    testCompile(libraries.power_mock_junit4)
//    testCompile(libraries.power_mock_junit4_rule)
//    testCompile(libraries.gson)
//}
