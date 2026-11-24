plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localAppData = providers.environmentVariable("LOCALAPPDATA").orNull
val checkout = rootDir.canonicalFile.toPath()
val isInOneDrive = listOf("OneDrive", "OneDriveConsumer", "OneDriveCommercial")
    .mapNotNull { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
    .any { checkout.startsWith(file(it).canonicalFile.toPath()) }

if (localAppData != null && isInOneDrive) {
    // Keep generated files outside OneDrive so Gradle can replace them during builds.
    val checkoutId = Integer.toHexString(checkout.hashCode())
    layout.buildDirectory.set(file("$localAppData/StageAcademy/build/$checkoutId/app"))
}

android {
    namespace = "fr.stageacademy.quiz"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.stageacademy.quiz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val cameraXVersion = "1.4.2"
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
