plugins {
    java
    checkstyle
    jacoco
    id("com.github.spotbugs") version "1.6.10"
    pmd
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

checkstyle {
    toolVersion = "8.15"
    configFile = file("codequality/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
}

jacoco {
    toolVersion = "0.8.3"
    reportsDir = file("$buildDir/reports/jacoco")
}

tasks.jacocoTestReport {
    reports {
        html.isEnabled = true
        html.destination = file("$buildDir/reports/jacoco")
    }
}

tasks {
    jacocoTestCoverageVerification {
        violationRules {
            rule { limit { minimum = BigDecimal.valueOf(0.0) } }
        }
    }
    check {
        dependsOn(jacocoTestCoverageVerification)
    }
}

spotbugs {
    isIgnoreFailures = true
}

pmd {
    isIgnoreFailures = true
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libraries:gateway-common"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.netflix.eureka:eureka-client:1.8.6")
    implementation("org.apache.httpcomponents:httpclient:4.5.7")
    implementation("io.swagger:swagger-jersey2-jaxrs:1.5.10")

    testImplementation("junit:junit:4.12")
}
