plugins {
    id("photobox.android.library")
    id("photobox.android.compose")
    id("ksp")
    id("hilt")
}

android {
    namespace = "com.photobox.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.junit5.api)
}
