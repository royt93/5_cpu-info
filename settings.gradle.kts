pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_PROJECT: project build.gradle.kts vẫn được phép khai báo repositories
    // (allprojects { repositories { ... } } trong root). Nếu chuyển sang FAIL_ON_PROJECT_REPOS
    // sẽ phải gỡ block đó ở root.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}

include(":app")
