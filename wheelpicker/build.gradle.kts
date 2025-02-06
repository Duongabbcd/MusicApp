plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.library")
}

android {
    namespace = "com.cheonjaeung.powerwheelpicker.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    api("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.cheonjaeung.simplecarousel.android:simplecarousel:0.3.0")
}
