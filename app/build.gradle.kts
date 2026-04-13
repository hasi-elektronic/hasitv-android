plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.hasi.hasitv"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.hasi.hasitv"
        minSdk = 21          // Android 5 — Fire TV Stick 1st gen support
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Android TV / Leanback
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // ExoPlayer (Media3)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.hls)
    implementation(libs.exoplayer.rtsp)
    implementation(libs.exoplayer.dash)
    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.session)

    // Network
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)

    // Image loading
    implementation(libs.coil.compose)

    // Room (local DB)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
