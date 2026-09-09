plugins {
    id("photobox.android.library")
    id("photobox.android.compose")
    id("ksp")
    id("hilt")
}

android {
    namespace = "com.photobox.feature.share"
    buildFeatures {
        buildConfig = true  // 用于 BuildConfig.WECHAT_APP_ID（虽然定义在 app 模块，但这里也开 buildConfig 以备后用）
    }
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
    implementation(libs.kotlinx.coroutines.android)

    // 微信 OpenSDK：本地 aar
    implementation(files("$rootDir/libs/open-sdk-lite-release.aar"))

    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
