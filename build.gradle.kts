import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    idea
    eclipse
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.ben-manes.versions")
    id("com.diffplug.spotless")
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    create("functionalTest") {
        java {
            compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            srcDir("src/functional-test/java")
        }
    }
}

idea {
    module {
        testSources.from(sourceSets["functionalTest"].java.srcDirs)
    }
}

val functionalTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val functionalTestRuntimeOnly: Configuration by configurations.getting

configurations {
    configurations["functionalTestImplementation"].extendsFrom(configurations.testImplementation.get())
    configurations["functionalTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())
}

val functionalTest = task<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"

    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()

    systemProperty("jacoco-agent.destfile", file("${layout.buildDirectory.get()}/jacoco/functionalTest.exec"))

    testLogging {
        events ("failed", "passed", "skipped", "standard_out")
    }
}

dependencies {
    implementation ("org.springframework.boot:spring-boot-starter-web")
    implementation ("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude (group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        events ("failed", "passed", "skipped", "standard_out")
    }
}

tasks.jacocoTestReport {
    description = "Generates code coverage report for both unit and functional tests."
    group = "verification"

    dependsOn(tasks.test, functionalTest)
    mustRunAfter(tasks.test, functionalTest)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec")
    )

    sourceDirectories.setFrom(files(sourceSets.main.get().allSource.srcDirs))
    classDirectories.setFrom(files(sourceSets.main.get().output))

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/Application.class")
                exclude("**/config/**")
            }
        })
    )

    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    description = "Verifies code coverage metrics."
    group = "verification"

    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "CLASS"
                value = "MISSEDCOUNT"
                maximum = "5".toBigDecimal()
            }
        }
    }
}

tasks.register("testWithCoverage") {
    description = "Runs all tests and generates coverage report."
    group = "verification"

    dependsOn(tasks.test, functionalTest, tasks.jacocoTestReport)

    doLast {
        println("Coverage report generated at: ${layout.buildDirectory.get()}/reports/jacoco/test/html/index.html")
    }
}

tasks.register("coverageSummary") {
    description = "Displays coverage summary in console."
    group = "verification"

    dependsOn(tasks.jacocoTestReport)

    doLast {
        val reportFile = file("${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        if (reportFile.exists()) {
            println("Coverage Summary:")
            println("HTML Report: ${layout.buildDirectory.get()}/reports/jacoco/test/html/index.html")
            println("XML Report: ${reportFile.absolutePath}")
        }
    }
}

tasks.check {
    dependsOn(functionalTest)
    dependsOn(tasks.jacocoTestReport)
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
    gradleReleaseChannel="current"
}

spotless {
    java {
        palantirJavaFormat()
        formatAnnotations()
    }
}

tasks.withType<Test> {
    systemProperty("jacoco-agent.destfile", file("${layout.buildDirectory.get()}/jacoco/${name}.exec"))
}