plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.tapscript.platform.android"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":engine-api"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
}
