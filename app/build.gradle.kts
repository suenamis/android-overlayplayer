plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.overlay_player"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.overlay_player"
        minSdk = 24
        targetSdk = 36
        // Die Vorgaben per -P dienen nur dem Update-Test auf dem Emulator.
        versionCode = (findProperty("overlayVersionCode") as String?)?.toInt() ?: 3
        versionName = findProperty("overlayVersionName") as String? ?: "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "UPDATE_BASE_URL", "\"https://bums.tail1c1957.ts.net:5241\"")
        buildConfigField("boolean", "UPDATE_ALLOW_CLEARTEXT", "false")
        manifestPlaceholders["cleartextTraffic"] = "false"
    }

    signingConfigs {
        create("release") {
            // Kein stilles Ersatzschlüssel-Verhalten: Fehlt die Datei, bricht der Bau ab.
            storeFile = file(
                System.getenv("OVERLAY_KEYSTORE")
                    ?: "E:/OneDrive/Dokumente/AppSchlüssel/overlay-player-debug.keystore"
            )
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("updateTest") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            isDebuggable = true
            buildConfigField("String", "UPDATE_BASE_URL", "\"http://10.0.2.2:5299\"")
            buildConfigField("boolean", "UPDATE_ALLOW_CLEARTEXT", "true")
            manifestPlaceholders["cleartextTraffic"] = "true"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.media)
    implementation(libs.androidx.documentfile)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}