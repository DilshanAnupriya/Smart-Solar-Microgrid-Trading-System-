plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mobileapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mobileapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // ZXing core: plain Java library used only to draw the reservation QR code
    implementation(libs.zxing.core)
    // Coroutines: API calls run on Dispatchers.IO and their results come back on the main thread
    implementation(libs.kotlinx.coroutines.android)
    // Google Maps SDK for Android
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    // Real org.json for JVM unit tests; the copy inside android.jar is only a stub there
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
