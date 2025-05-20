plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hcmute.edu.vn.thongvavan.finalproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "hcmute.edu.vn.thongvavan.finalproject"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // To recognize Latin script
    implementation ("com.google.mlkit:text-recognition:16.0.1")
    // To recognize Chinese script
    implementation ("com.google.mlkit:text-recognition-chinese:16.0.1")
    // To recognize Devanagari script
    implementation ("com.google.mlkit:text-recognition-devanagari:16.0.1")
    // To recognize Japanese script
    implementation ("com.google.mlkit:text-recognition-japanese:16.0.1")
    // To recognize Korean script
    implementation ("com.google.mlkit:text-recognition-korean:16.0.1")
    
    // MVVM Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.7.0")
    
    // Optional: for annotation processor
    annotationProcessor("androidx.lifecycle:lifecycle-compiler:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}