plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.photobox.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}