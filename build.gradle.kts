// Root plugin version alignment for all subprojects
plugins {
    kotlin("multiplatform") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.compose") version "1.9.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
