plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.tapscript.engine.core"
    compileSdk = 37

    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":engine-api"))
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}
