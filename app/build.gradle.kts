plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.starfleet.idle"
    compileSdk = 35

    defaultConfig {
        applicationId = "uk.co.royhillis.starfleetidle"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.2.3"
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Don't abort the release build on lint warnings — most of these are
        // suggestions, not actual bugs. We'll address them iteratively.
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    implementation("com.google.android.gms:play-services-games-v2:20.1.2")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
