plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.alexsmedstimer"
    compileSdk = 36

    defaultConfig {
        // Permanent public identity. The Java namespace stays unchanged to avoid
        // unnecessary changes to the tested scheduling and receiver code.
        applicationId = "io.github.g10112.medstimer"
        minSdk = 34
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("medsRelease") {
            val keystorePath = System.getenv("MEDS_RELEASE_KEYSTORE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MEDS_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("MEDS_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("MEDS_RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("medsRelease")
            isMinifyEnabled = false
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
}

dependencies {
    // The app uses Android Views and Java. No Kotlin or Compose is needed.
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
