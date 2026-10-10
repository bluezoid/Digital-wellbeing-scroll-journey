import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Release signing: the key lives OUTSIDE the repo (../scroll-journey-keys/, never committed).
 * Without it (e.g. on another machine) the release build is simply left unsigned.
 */
val releaseKeyProperties = rootProject.file("../scroll-journey-keys/keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

android {
    namespace = "com.achyutpal.scrolljourney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.achyutpal.scrolljourney"
        minSdk = 26
        targetSdk = 36
        // versionCode = major * 10000 + minor * 100 + patch (must grow with every release).
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        // Lets MigrationTestHelper read the exported schema JSON files in on-device tests.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    signingConfigs {
        releaseKeyProperties?.let { key ->
            create("release") {
                storeFile = file(key.getProperty("storeFile"))
                storePassword = key.getProperty("storePassword")
                keyAlias = key.getProperty("keyAlias")
                keyPassword = key.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // A separate app ("SJ Dev") so development builds never collide with the real install.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
        release {
            // R8: removes unused code/resources and shortens names (smaller, faster app).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // One APK per phone chip type instead of one 45 MB APK with all of them.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // Room writes a JSON snapshot of every schema version here (commit it!), needed for migration tests.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    constraints {
        // Lifecycle brings 1.7.3, but room-testing (migration tests) needs 1.8.x, and test code
        // must use the app's version: align the app on 1.8.1 (backwards compatible).
        implementation(libs.kotlinx.serialization.core)
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Basic icons (back, menu, refresh, chevrons); no longer pulled in by material3 itself.
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Declares an empty test activity in debug builds for Compose UI tests (createComposeRule).
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    // Backup file (export / import) as JSON.
    implementation(libs.kotlinx.serialization.json)

    // Home-screen widgets (Jetpack Glance: Compose-style code rendered as RemoteViews by the launcher).
    implementation(libs.androidx.glance.appwidget)

    // 3D: SceneView (Compose wrapper around Google's Filament rendering engine)
    implementation(libs.sceneview)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
