package com.photobox.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            val commonExtension = extensions.findByType(CommonExtension::class.java)
                ?: error("AndroidComposePlugin must be applied AFTER android application/library plugin")
            commonExtension.apply {
                buildFeatures {
                    compose = true
                }
                dependencies {
                    add("implementation", platform(libs.androidx.compose.bom))
                    add("androidTestImplementation", platform(libs.androidx.compose.bom))
                }
            }
        }
    }
}