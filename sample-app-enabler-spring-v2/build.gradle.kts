plugins {
    java
    id("org.springframework.boot") version "2.1.3.RELEASE"
}
apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":message-enabler-spring-v2"))
    implementation(project(":gateway-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}


//dependencies {
//    compile(project(":message-enabler-spring-v2"))
//    compile(project(":gateway-common"))
//
//    compile libraries.gson
//    compile libraries.spring_boot_starter_actuator
//    compile libraries.spring_boot_starter_web
//    compile libraries.spring_boot_starter_websocket
//    compile libraries.spring_cloud_starter_eureka
//
//    testCompile libraries.spring_boot_starter_test
//}
