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
 * Release signing, for the APKs this project publishes itself — a GitHub release, a build
 * handed to someone to try. Android will not install an unsigned APK at all, so without
 * this `assembleRelease` produces a file that cannot be used for anything.
 *
 * Values come from a gitignored `keystore.properties` at the repository root for a local
 * build, or from the matching environment variables in CI. With neither present the
 * release build is left unsigned rather than failing, because `assembleRelease` has to
 * keep working for CI and for anyone who has just cloned the repository and has no key.
 *
 * The keystore itself never enters the repository, and neither do the passwords. Losing
 * it means never being able to publish an update: Android identifies an app by its
 * signature, and there is no recovery process.
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
        // 0.1.0 (code 1) was a signed build of 15 October that was never published. The
        // first public release is 0.2.0 so nothing can mistake it for that one.
        versionCode = 2
        versionName = "0.2.0"
    }

    // AGP otherwise writes the dependency list into the APK signing block, encrypted with
    // a key only Google can read. F-Droid's scanner flags it, and an app that promises to
    // send nothing anywhere has no business carrying a payload its users cannot read.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        val storePath = signingSecret("storeFile", "MINTAL_KEYSTORE_FILE")
        val storePassword = signingSecret("storePassword", "MINTAL_KEYSTORE_PASSWORD")
        val keyAlias = signingSecret("keyAlias", "MINTAL_KEY_ALIAS")
        val keyPassword = signingSecret("keyPassword", "MINTAL_KEY_PASSWORD")
        if (storePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                // An absolute path is taken as-is, so CI can point at a keystore it
                // decoded into a temporary directory outside the checkout.
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
            // Null when no keystore was configured, which leaves the APK unsigned and
            // therefore uninstallable. The release workflow checks for that rather than
            // publishing one by accident.
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
