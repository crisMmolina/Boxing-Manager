plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sigebox.juez"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sigebox.juez"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        // Robolectric necesita los recursos y el Manifest fusionado.
        unitTests.isIncludeAndroidResources = true
        // Robolectric con SDK 36 toca jdk.internal.access (ApplicationSharedMemory) y JDK 17+ no lo exporta.
        unitTests.all {
            it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            // En CI solo se veía "1 failed": que diga qué prueba falló y por qué (AND-008).
            it.testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                // tools/smoke.sh corre con -q y el nivel quiet oculta lo de arriba (AND-010).
                quiet {
                    events("failed")
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                }
            }
        }
    }
}

dependencies {
    implementation(libs.sigebox.core)

    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.okhttp)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    // Modelo incluido en el APK: escanea sin internet ni Google Play Services.
    implementation(libs.mlkit.barcode)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
