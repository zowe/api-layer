plugins {
    java
    id("org.springframework.boot") version "2.1.3.RELEASE"
}
apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:2.1.0.RELEASE")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server:2.1.0.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

