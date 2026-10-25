// core/ es un build independiente (ADR 0001), consumido por app/ y BOXCENTRAL/ como composite build.
// Al incluirse corre con el Gradle del build anfitrión (hoy 8.2 en la raíz): usar solo APIs
// compatibles con Gradle 8 (`.set(...)`, sin toolchains).
plugins {
    `java-library`
    jacoco
}

group = "com.sigebox"
version = "0.1.0"

repositories {
    mavenCentral()
}

val gsonVersion = project.property("gsonVersion") as String
val junitVersion = project.property("junitVersion") as String
val jsonSchemaValidatorVersion = project.property("jsonSchemaValidatorVersion") as String
val jacocoVersion = project.property("jacocoVersion") as String

dependencies {
    api("com.google.code.gson:gson:$gsonVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = jacocoVersion
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
