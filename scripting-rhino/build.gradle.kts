plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.tapscript.scripting.rhino"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":engine-api"))
    implementation(libs.rhino)
    implementation(libs.gson)
    testImplementation(libs.junit)
}
