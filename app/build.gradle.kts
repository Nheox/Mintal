import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
}

/**
 * Release signing for the APKs this project publishes itself — a GitHub release, a build
 * handed to a tester. F-Droid never uses it: F-Droid builds from source and signs with
 * its own key, which is the whole point of the store.
 *
 * The values come from a gitignored `keystore.properties` at the repository root, or
 * from the matching environment variables for CI. When neither is present the release
 * build is left unsigned rather than failing, because `assembleRelease` has to keep
 * working for CI, for F-Droid and for anyone who just cloned the repository.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingSecret(property: String, environment: String): String? =
    (keystoreProperties.getProperty(property) ?: System.getenv(environment))
        ?.takeIf { it.isNotBlank() }

android {
    namespace = "dev.nheox.mintal"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.nheox.mintal"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        val storePath = signingSecret("storeFile", "MINTAL_KEYSTORE_FILE")
        val storePassword = signingSecret("storePassword", "MINTAL_KEYSTORE_PASSWORD")
        val keyAlias = signingSecret("keyAlias", "MINTAL_KEY_ALIAS")
        val keyPassword = signingSecret("keyPassword", "MINTAL_KEY_PASSWORD")
        if (storePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                this.storeFile = rootProject.file(storePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // Null when no keystore was configured, which leaves the APK unsigned.
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        // Robolectric renders real Compose UI on the JVM, which needs the app's resources
        // on the unit-test classpath.
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// The Room plugin, rather than a bare room.schemaLocation argument: it gives each variant
// its own schema directory. Pointing the debug and release KSP tasks at one directory makes
// them race, and the loser reads a half-written file.
room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Screenshot rendering: Robolectric draws Compose on the JVM, so the UI can be looked
    // at without an emulator or a device. Robolectric is JUnit 4, hence the vintage engine
    // alongside the JUnit 5 tests.
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
