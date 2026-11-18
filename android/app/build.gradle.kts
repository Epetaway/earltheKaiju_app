plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

// Signing keys never live in this repository. CI unpacks the ones a job needs from the
// KAIJU_SIGNING_BUNDLE secret into a temporary folder and points KAIJU_SIGNING_DIR at it:
//   release.p12  + release.password   the personal build installed on Earl's phone
//   ci-debug.p12 + ci-debug.password  CI debug builds, so each one updates the last in place
// Without it (Android Studio, a fresh clone) debug builds use the local debug key and the
// release APK comes out unsigned. See docs/10-personal-builds.md.
val signingDir: File? = providers.environmentVariable("KAIJU_SIGNING_DIR").orNull
    ?.let(::File)?.takeIf { it.isDirectory }

fun signingKey(name: String): Pair<File, String>? {
    val dir = signingDir ?: return null
    val store = dir.resolve("$name.p12").takeIf { it.isFile } ?: return null
    val password = dir.resolve("$name.password").takeIf { it.isFile }?.readText()?.trim() ?: return null
    return store to password
}

// CI passes its run number, so every build has a higher version code than the one before and
// installs over it. Local builds are 1.
val buildNumber = providers.environmentVariable("KAIJU_BUILD_NUMBER").orNull?.toIntOrNull() ?: 1

android {
    namespace = "com.earlthekaiju.coach"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.earlthekaiju.coach"
        minSdk = 26
        // Android 16 behavior. Move to 37 once Android 17 behavior changes are reviewed.
        targetSdk = 36
        versionCode = buildNumber
        versionName = "0.1.0"
        // Swaps in a test Application (fresh in-memory data, a clock starting on a known evening).
        testInstrumentationRunner = "com.earlthekaiju.coach.device.KaijuTestRunner"
    }

    signingConfigs {
        signingKey("release")?.let { (store, password) ->
            create("personal") {
                storeFile = store
                storeType = "PKCS12"
                storePassword = password
                keyAlias = "kaiju-coach-release"
                keyPassword = password
            }
        }
        signingKey("ci-debug")?.let { (store, password) ->
            create("ciDebug") {
                storeFile = store
                storeType = "PKCS12"
                storePassword = password
                keyAlias = "kaiju-coach-ci-debug"
                keyPassword = password
            }
        }
    }

    buildTypes {
        debug {
            // Installs beside the personal build as "Kaiju Test", with its own data.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfigs.findByName("ciDebug")?.let { signingConfig = it }
        }
        release {
            // The personal build: no sample-data tools (they are debug-only), signed in CI.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("personal")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                test.maxHeapSize = "3g"
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        // Version-currency checks need network access to Google Maven; dependency
        // versions are verified by hand in docs/09-dependency-verification.md.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.room.testing)

    // Instrumented tests on an emulator or phone (CI "Device run" job).
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose's test library brings an older Espresso that crashes on Android 17 (it reflects on
    // InputManager.getInstance, which is gone). 3.7.0 uses the public API.
    androidTestImplementation(libs.androidx.test.espresso.core)
}
