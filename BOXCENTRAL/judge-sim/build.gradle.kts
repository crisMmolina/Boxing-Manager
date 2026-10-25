// Simulador de jueces por línea de comandos (plan B de la demo, medición de latencia).
plugins {
    application
}

val junitVersion = project.property("junitVersion") as String
val slf4jVersion = project.property("slf4jVersion") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "com.sigebox.judgesim.JudgeSim"
}

dependencies {
    implementation("com.sigebox:core")
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
