plugins {
    `kotlin-dsl`
}

group = "com.photobox.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "photobox.android.application"
            implementationClass = "com.photobox.convention.AndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = "photobox.android.library"
            implementationClass = "com.photobox.convention.AndroidLibraryPlugin"
        }
        register("androidCompose") {
            id = "photobox.android.compose"
            implementationClass = "com.photobox.convention.AndroidComposePlugin"
        }
    }
}
