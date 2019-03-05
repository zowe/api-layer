plugins {
    java
    id("org.springframework.boot") version "2.1.3.RELEASE"
}
apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:2.1.0.RELEASE")
    implementation("org.springframework.security:spring-security-web:5.1.4.RELEASE")
    implementation("org.springframework.security:spring-security-config:5.1.4.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-security-test:5.1.4.RELEASE")
}

