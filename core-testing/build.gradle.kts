plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.construccionia.core.testing"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    implementation(project(":core-common"))
    implementation(project(":feature-generation"))
    implementation(project(":feature-ocr"))
    implementation(project(":feature-export"))
    implementation(project(":feature-viewer"))
    implementation(project(":feature-animation"))

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.test)

    // Mockk
    implementation(libs.mockk)

    // JUnit
    implementation(libs.junit)
}
