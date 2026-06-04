plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.mchav"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // The AGP variant API used by FregeAndroidPlugin. compileOnly is correct for
    // an included/published Gradle plugin: the real AGP is on the consuming
    // project's plugin classpath at apply time.
    compileOnly("com.android.tools.build:gradle-api:8.13.2")
}

gradlePlugin {
    plugins {
        create("froid") {
            id = "io.github.mchav.froid"
            implementationClass = "io.github.mchav.froid.gradle.FregeAndroidPlugin"
            displayName = "froid Frege/Android plugin"
            description = "Compiles Frege sources in an Android project (the modern froid.gradle)."
        }
    }
}
