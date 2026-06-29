plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.threatfabric.assessment.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.threatfabric.assessment.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-android:1.8.2")
    implementation("androidx.compose.runtime:runtime-android:1.8.2")
    implementation("androidx.compose.foundation:foundation-android:1.8.2")
    implementation("androidx.compose.material:material-android:1.8.2")
    implementation(project(":storage"))
}
