plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-library"))
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    implementation("ch.qos.logback:logback-classic:1.2.3")


    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("org.apache.commons:commons-lang3:3.8.1")
    implementation("org.springframework.security:spring-security-web:5.1.4.RELEASE")
    implementation("org.springframework.security:spring-security-config:5.1.4.RELEASE")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.8")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.24.5")
    testImplementation("org.springframework.security:spring-security-test:5.1.4.RELEASE")



//
//        compileOnly libraries.slf4j_api
//        compileOnly libraries.javax_servlet_api
//
//        testCompile libraries.hamcrest
//        testCompile libraries.logback_classic
//        testCompile libraries.javax_servlet_api
}
