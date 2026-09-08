plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.photobox.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}