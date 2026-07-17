plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
}

android{
    compileSdk = 36
    namespace = "com.eitan.trainer"
     defaultConfig {
        applicationId = "com.eitan.trainer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file(".debug-keystore/debug.keystore")
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
}
    

