plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.voicemailsender"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.voicemailsender"
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
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.json)
    implementation(libs.material)
    implementation(libs.corektx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Gson converter for Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // (Optional but useful) OkHttp Logging Interceptor for debugging API
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Other existing dependencies

        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
// Gemini API (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // JSON parsing
    implementation("org.json:json:20210307")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Kotlin extensions
    implementation("androidx.core:core-ktx:1.12.0")

    // AndroidX support
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Optional: Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}