plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.duy.sentinelai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duy.sentinelai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Thư viện gọi API siêu tốc
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coroutines để chạy ngầm không làm đơ UI
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}