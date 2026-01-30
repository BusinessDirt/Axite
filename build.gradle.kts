plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.3" apply false
}

group = "github.businessdirt"

allprojects {
    repositories {
        mavenCentral()
    }
}