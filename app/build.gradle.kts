plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
// Apply the Google Services plugin
    id("com.google.gms.google-services")
// Apply the Firebase Crashlytics plugin
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.musicapp"
    compileSdk = 34
    useLibrary ("org.apache.http.legacy")
    defaultConfig {
        applicationId = "com.example.musicapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(files("libs/searchapi_all_lua-release_v20.09.2024.aar"))
    implementation(files("libs/serverconfig_ms_opensource-release_v07.09.2024.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":service"))
    implementation(project(":wheelpicker"))
    implementation(project(":scrollbar"))

    //exo player
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation( "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //gson
    implementation("com.google.code.gson:gson:2.11.0")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor( "com.github.bumptech.glide:compiler:4.12.0")

    //Google Play
    implementation("com.google.android.play:app-update:2.1.0")

    //Animation
    implementation( "com.airbnb.android:lottie:6.5.2")

    //Draggable
    implementation( "com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    //call api
    implementation( "com.squareup.okhttp3:okhttp:4.12.0")

    //Lua
    implementation( "org.mozilla:rhino:1.7.15")
    implementation( "com.android.installreferrer:installreferrer:2.2")
    implementation( "com.google.android.gms:play-services-appset:16.1.0")

    //Google Firebase Crash Analytics
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    // Firebase SDK for Google Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")
    // Firebase Crashlytics SDK
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}