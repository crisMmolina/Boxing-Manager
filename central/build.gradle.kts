// App central (ADR 0001): JDK 25 + JavaFX 25, consume core por composite build.
plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.sigebox"
version = "0.1.0"

val javafxVersion = project.property("javafxVersion") as String
val javalinVersion = project.property("javalinVersion") as String
val sqliteJdbcVersion = project.property("sqliteJdbcVersion") as String
val zxingVersion = project.property("zxingVersion") as String
val openpdfVersion = project.property("openpdfVersion") as String
val slf4jVersion = project.property("slf4jVersion") as String
val junitVersion = project.property("junitVersion") as String

allprojects {
    repositories {
        mavenCentral()
    }
    // `test --tests X` corre en todos los subproyectos: sin esto judge-sim falla con "No tests found".
    tasks.withType<Test>().configureEach {
        filter.isFailOnNoMatchingTests = false
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls")
}

application {
    mainClass = "com.sigebox.central.App"
}

dependencies {
    implementation("com.sigebox:core")
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.google.zxing:javase:$zxingVersion")
    implementation("com.github.librepdf:openpdf:$openpdfVersion")
    implementation("org.dhatim:fastexcel:0.20.2") // DSK-052: .xlsx de resultados; aprobada por el humano
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation(project(":judge-sim")) // QA-001: jueces simulados contra el servidor real
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.dhatim:fastexcel-reader:0.20.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// Instalador Windows con runtime incluido (spike DSK-002). Solo corre en Windows.
// .msi y .exe necesitan WiX v3 (candle/light) o v4+ (wix) en el PATH; sin WiX: -PinstallerType=app-image.
// jpackage no acepta --class-path: toma todas las jars planas de --input (lib/ de installDist).
val jpackage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Empaqueta la central con jpackage (.msi por defecto)."
    dependsOn(tasks.installDist)
    onlyIf { System.getProperty("os.name").startsWith("Windows") }

    val installerType = providers.gradleProperty("installerType").orElse("msi")
    val launcher = javaToolchains.launcherFor(java.toolchain)
    val installDir = layout.buildDirectory.dir("install/BOXCENTRAL/lib")
    val destDir = layout.buildDirectory.dir("jpackage")
    val mainJar = tasks.jar.flatMap { it.archiveFileName }

    doFirst {
        delete(destDir)
        val jdkBin = launcher.get().metadata.installationPath.dir("bin")
        commandLine(
            jdkBin.file("jpackage").asFile.absolutePath,
            "--type", installerType.get(),
            "--name", "Box Central",
            "--app-version", project.version.toString(),
            "--vendor", "SIGEBOX",
            "--input", installDir.get().asFile.absolutePath,
            "--main-jar", mainJar.get(),
            "--main-class", "com.sigebox.central.App",
            // JavaFX carga sus DLL desde el classpath; sin esto JDK 25 avisa en cada arranque.
            "--java-options", "--enable-native-access=ALL-UNNAMED",
            "--dest", destDir.get().asFile.absolutePath,
        )
        if (installerType.get() != "app-image") {
            args("--win-menu", "--win-shortcut", "--win-dir-chooser")
        }
    }
}
