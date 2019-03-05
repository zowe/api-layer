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
            rule { limit { minimum = BigDecimal.valueOf(0.6) } }
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
    testImplementation("junit:junit:4.12")
}


//checkstyle {
//    toolVersion = "8.12"
//    configFile = rootProject.file("codequality/checkstyle/checkstyle.xml")
//    configProperties = [
//        "configDir": rootProject.file("codequality/checkstyle"),
//    "baseDir"  : rootDir,
//    ]
//}
