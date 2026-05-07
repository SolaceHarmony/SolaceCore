// Root plugin version alignment for all subprojects
plugins {
    kotlin("multiplatform") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    kotlin("plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.compose") version "1.11.0-rc01" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
