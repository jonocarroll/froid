plugins {
    id("com.android.library")
    id("io.github.mchav.froid")
    `maven-publish`
}

group = "io.github.mchav"
version = project.findProperty("froidVersion") as String? ?: "0.1.0"

android {
    namespace = "io.github.mchav.froid"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Publish the release AAR (consumed as io.github.mchav:froid / via JitPack).
    publishing {
        singleVariant("release")
    }
}

// The Frege runtime is provided to consumers by the `io.github.mchav.froid`
// Gradle plugin (it downloads + adds the snapshot jar), so the library AAR needs
// no Frege dependency of its own.

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                artifactId = "froid"
            }
        }
    }
}
