plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.kotlin.multiplatform.library") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.3.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
}

val localAppData = providers.environmentVariable("LOCALAPPDATA").orNull
val checkout = rootDir.canonicalFile.toPath()
val isInOneDrive = listOf("OneDrive", "OneDriveConsumer", "OneDriveCommercial")
    .mapNotNull { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
    .any { checkout.startsWith(file(it).canonicalFile.toPath()) }

if (localAppData != null && isInOneDrive) {
    // Keep generated files outside OneDrive so Gradle can replace them during builds.
    val checkoutId = Integer.toHexString(checkout.hashCode())
    subprojects {
        layout.buildDirectory.set(file("$localAppData/StageAcademy/build/$checkoutId/$name"))
    }
}
