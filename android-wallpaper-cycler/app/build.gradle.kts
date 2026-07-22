plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jonabadie.wallpapercycler"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jonabadie.wallpapercycler"
        minSdk = 24
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as? String) ?: "1.0-dev"
    }

    // Every build must have the same signature or Android refuses to install
    // updates over the previous version. CI restores this keystore from the
    // SIGNING_KEYSTORE_B64 repo secret; without it we fall back to the default
    // debug key (builds fine, but updates won't install over other builds).
    val sharedKeystore = rootProject.file("signing.keystore")
    signingConfigs {
        create("shared") {
            storeFile = sharedKeystore
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "wallpaper123"
            keyAlias = "wallpaper"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: "wallpaper123"
        }
    }

    buildTypes {
        debug {
            if (sharedKeystore.exists()) signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            if (sharedKeystore.exists()) signingConfig = signingConfigs.getByName("shared")
        }
    }

    buildFeatures {
        buildConfig = true
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
