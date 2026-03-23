import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Libs.GradleVersion.plugin) version Libs.GradleVersion.version
}

buildscript {

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(Libs.androidGradlePlugin)
        classpath(Libs.Kotlin.gradlePlugin)
        classpath(Libs.Hilt.gradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // AdmobWrapper SDK
    }
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            force("com.google.android.gms:play-services-ads:23.6.0")
        }
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            @Suppress("SuspiciousCollectionReassignment")
            freeCompilerArgs += listOf(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
//            allWarningsAsErrors = true
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }
}

/**
 * Update dependencyUpdates task to reject versions which are more 'unstable' than our
 * current version.
 */
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        val current = DependencyUpdates.versionToRelease(currentVersion)
        // If we're using a SNAPSHOT, ignore since we must be doing so for a reason.
        if (current == ReleaseType.SNAPSHOT) {
            true
        } else {
            // Otherwise we reject if the candidate is more 'unstable' than our version
            DependencyUpdates.versionToRelease(candidate.version).isLessStableThan(current)
        }
    }
}
