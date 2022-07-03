plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    compileSdk = 32
    buildToolsVersion = "33.0.0"

    defaultConfig {
        applicationId = "ir.mahdiparastesh.mobinaexplorer"
        minSdk = 28
        targetSdk = 31
        versionCode = 1
        versionName = "4.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
}

@Suppress("SpellCheckingInspection")
dependencies {
    val roomVersion = "2.4.2"

    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0-alpha01")
    implementation("androidx.activity:activity-ktx:1.5.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // Gradle doesn't only include used classes; it compiles everything!!
    // We've gotta sabotage "com.google.android.datatransport"!!
}
