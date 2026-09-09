plugins {
    id("photobox.android.library")
    id("photobox.android.compose")
    id("ksp")
    id("hilt")
}

android {
    namespace = "com.photobox.feature.profile"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)

    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}