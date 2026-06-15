plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.prplegryn.verbo"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.prplegryn.verbo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("verbo") {
            storeFile = file("signing/verbo-release.p12")
            storePassword = "verbo-release-2026"
            storeType = "PKCS12"
            keyAlias = "verbo"
            keyPassword = "verbo-release-2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("verbo")
        }
        release {
            signingConfig = signingConfigs.getByName("verbo")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
            )
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("org.jetbrains.compose.ui:ui:1.11.1")
    implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.11.1")
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.11.1")

    implementation(files("libs/miuix-core-android-0.9.2.aar"))
    implementation(files("libs/miuix-shader-android-0.9.2.aar"))
    implementation(files("libs/miuix-squircle-android-0.9.2.aar"))
    implementation(files("libs/miuix-ui-android-0.9.2.aar"))
    implementation(files("libs/miuix-icons-android-0.9.2.aar"))
    implementation(files("libs/miuix-preference-android-0.9.2.aar"))
    implementation("org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0")
    implementation("org.jetbrains.compose.material3:material3-window-size-class:1.9.0")
    implementation("com.materialkolor:material-color-utilities-android:4.1.1")

    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
