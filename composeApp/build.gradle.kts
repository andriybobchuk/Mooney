import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Link UIKit explicitly so the framework's modulemap gets a
            // `use UIKit` — without this, Swift sees the protocol's
            // `UIView` return as a forward-declared opaque class, and any
            // Swift impl using `UIKit.UIView` fails to conform to the
            // protocol (this took an hour to diagnose).
            linkerOpts("-framework", "UIKit")
        }
    }


    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.billing.ktx)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.google.ads)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)

            implementation(libs.bundles.ktor)
            implementation(libs.bundles.coil)
            implementation("androidx.datastore:datastore-preferences-core:1.1.1")

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            implementation("sh.calvin.reorderable:reorderable:2.4.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.koin.test)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        dependencies {
            ksp(libs.androidx.room.compiler)
        }
    }
}

android {
    namespace = "com.andriybobchuk.mooney"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.andriybobchuk.mooney"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Both versions come from gradle.properties so they stay in lock-step
        // with the iOS MARKETING_VERSION / bundle version.
        versionCode = (project.findProperty("app.versionCode") as String).toInt()
        versionName = project.findProperty("app.version") as String
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Release signing reads from environment variables (CI) or local.properties
    // (developer machine). All four envs must be present to enable signed release
    // builds; otherwise unsigned debug-style builds are still possible.
    val keystoreFileEnv = System.getenv("MOONEY_KEYSTORE_FILE")
        ?: project.findProperty("MOONEY_KEYSTORE_FILE") as String?
    val keystorePasswordEnv = System.getenv("MOONEY_KEYSTORE_PASSWORD")
        ?: project.findProperty("MOONEY_KEYSTORE_PASSWORD") as String?
    val keyAliasEnv = System.getenv("MOONEY_KEY_ALIAS")
        ?: project.findProperty("MOONEY_KEY_ALIAS") as String?
    val keyPasswordEnv = System.getenv("MOONEY_KEY_PASSWORD")
        ?: project.findProperty("MOONEY_KEY_PASSWORD") as String?
    val releaseSigningAvailable = listOf(
        keystoreFileEnv, keystorePasswordEnv, keyAliasEnv, keyPasswordEnv
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(keystoreFileEnv!!)
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Patch the iOS framework so Swift can resolve `UIView` in protocol methods
// to `UIKit.UIView` instead of a forward-declared opaque type. Two parts:
//   1. modulemap gets `use UIKit` — declares the module dependency
//   2. umbrella header gets `#import <UIKit/UIKit.h>` — actually pulls in
//      UIKit's type declarations, which unifies the forward-declared
//      `@class UIView` with the real `UIKit.UIView`
// Without BOTH, Swift protocol conformance fails for any class returning a
// `UIKit.UIView` because Swift sees the protocol's `UIView` as a different
// (framework-local opaque) type. KMP provides no public knob for this, so
// we post-process.
tasks.matching { it.name.startsWith("link") && it.name.contains("FrameworkIos") }.configureEach {
    doLast {
        val frameworkBaseDir = outputs.files.singleFile
        val modulemap = frameworkBaseDir.resolve("ComposeApp.framework/Modules/module.modulemap")
        if (modulemap.exists()) {
            val content = modulemap.readText()
            if (!content.contains("use UIKit")) {
                modulemap.writeText(content.replace("use Foundation", "use Foundation\n    use UIKit"))
            }
        }
        val umbrellaHeader = frameworkBaseDir.resolve("ComposeApp.framework/Headers/ComposeApp.h")
        if (umbrellaHeader.exists()) {
            val content = umbrellaHeader.readText()
            if (!content.contains("#import <UIKit/UIKit.h>")) {
                // Insert right before the first Foundation #import so the
                // UIKit headers land at the top of the file with the rest of
                // the system framework imports.
                umbrellaHeader.writeText(
                    content.replaceFirst(
                        "#import <Foundation/NSArray.h>",
                        "#import <UIKit/UIKit.h>\n#import <Foundation/NSArray.h>"
                    )
                )
            }
        }
    }
}

// Generate AppVersion.kt from gradle.properties.
// `inputs.property("version", version)` is critical: without it the task
// has no declared inputs, so Gradle marks it UP-TO-DATE forever and the
// regenerated file silently stays on the old version after a bump.
val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version/kotlin")
    val version = findProperty("app.version") as String
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("com/andriybobchuk/mooney")
        dir.mkdirs()
        dir.resolve("AppVersion.kt").writeText(
            """
            package com.andriybobchuk.mooney

            const val APP_VERSION = "$version"
            """.trimIndent()
        )
    }
}

kotlin.sourceSets.getByName("commonMain") {
    kotlin.srcDir(generateVersionFile.map { layout.buildDirectory.dir("generated/version/kotlin") })
}

dependencies {
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.material3.android)
    debugImplementation(compose.uiTooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
}

compose.desktop {
    application {
        mainClass = "com.andriybobchuk.mooney.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.andriybobchuk.mooney"
            packageVersion = "1.0.0"
        }
    }
}
