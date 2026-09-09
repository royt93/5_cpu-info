import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Single source of truth cho mọi secret (signing + AppLovin + AdMob release + VIP backup) —
// xem doc/AD_PROMPT_AOS.MD Step 1.5. File nằm ở private repo royt93/myKeyStore, KHÔNG commit
// vào repo này.
val adsPropertiesFile =
    rootProject.file("../../../../myKeyStore/com.galaxyjoy.cpuinfo/ads.properties")
val adsPropertiesText = providers.fileContents(
    layout.file(providers.provider { adsPropertiesFile })
).asText.orNull
val adsProperties = Properties().apply {
    if (adsPropertiesText != null) {
        adsPropertiesText.reader().use(::load)
    }
}
val releaseTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
    val taskName = requestedTask.substringAfterLast(':')
    taskName.contains("release", ignoreCase = true) ||
        taskName.equals("build", ignoreCase = true) ||
        taskName.equals("assemble", ignoreCase = true) ||
        taskName.equals("bundle", ignoreCase = true)
}

if (releaseTaskRequested && !adsPropertiesFile.isFile) {
    throw GradleException(
        "Release signing configuration is missing. Clone the private royt93/myKeyStore " +
            "repository to ${adsPropertiesFile.parentFile.parentFile.parentFile} " +
            "and ensure com.galaxyjoy.cpuinfo/ads.properties exists."
    )
}

// debug build không bắt buộc có file (contributor chưa clone myKeyStore vẫn build debug được) —
// chỉ log cảnh báo, AppLovin ad sẽ không hiện (SDK fail-soft) thay vì crash.
fun adsProp(key: String): String =
    adsProperties.getProperty(key) ?: "".also {
        logger.warn("⚠️ ads.properties thiếu key '$key' (${adsPropertiesFile.path}) — AppLovin ad sẽ không hiện ở build này.")
    }

android {
    namespace = "com.galaxyjoy.cpuinfo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.galaxyjoy.cpuinfo"

        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 37
        versionCode = 20260907
        versionName = "2026.09.07"

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }

        // AppLovin MAX — giá trị thật đọc từ ads.properties (private myKeyStore repo), KHÔNG
        // hardcode ở đây. Xem adsProp() phía trên.
        buildConfigField("String", "APPLOVIN_SDK_KEY",     "\"${adsProp("applovinSdkKey")}\"")
        buildConfigField("String", "APPLOVIN_BANNER_ID",   "\"${adsProp("applovinBannerId")}\"")
        buildConfigField("String", "APPLOVIN_INTER_ID",    "\"${adsProp("applovinInterstitialId")}\"")
        buildConfigField("String", "APPLOVIN_APP_OPEN_ID", "\"${adsProp("applovinAppOpenId")}\"")
        buildConfigField("String", "APPLOVIN_REWARD_ID",   "\"${adsProp("applovinRewardedId")}\"")

        // VIP token ECDSA public key (verify-only, an toàn khi ship trong app) — private key
        // KHÔNG bao giờ đi qua BuildConfig, xem ads.properties.
        buildConfigField("String", "VIP_TOKEN_PUBLIC_KEY", "\"${adsProp("vipTokenPublicKey")}\"")

        // Privacy Policy URL — dùng chung cho consent dialog + VIP footer
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://loitp.notion.site/loitp/Privacy-Policy-319b1cd8783942fa8923d2a3c9bce60f/\""
        )
    }

//    ndkVersion = "25.1.8937393"
    ndkVersion = "26.3.11579264"

    signingConfigs {
        getByName("debug") {
//            val debugSigningConfig = SigningConfig.getDebugProperties(rootProject.rootDir)
//            storeFile = file(debugSigningConfig.getProperty(SigningConfig.KEY_PATH))
//            keyAlias = debugSigningConfig.getProperty(SigningConfig.KEY_ALIAS)
//            keyPassword = debugSigningConfig.getProperty(SigningConfig.KEY_PASS)
//            storePassword = debugSigningConfig.getProperty(SigningConfig.KEY_PASS)
        }
        if (adsPropertiesFile.isFile) create("release") {
//            val releaseSigningConfig = SigningConfig.getReleaseProperties(rootProject.rootDir)
//            storeFile = file(releaseSigningConfig.getProperty(SigningConfig.KEY_PATH))
//            keyAlias = releaseSigningConfig.getProperty(SigningConfig.KEY_ALIAS)
//            keyPassword = releaseSigningConfig.getProperty(SigningConfig.KEY_PASS)
//            storePassword = releaseSigningConfig.getProperty(SigningConfig.KEY_PASS)

            fun requiredSigningProperty(name: String): String =
                adsProperties.getProperty(name)?.takeIf(String::isNotBlank)
                    ?: throw GradleException(
                        "Missing '$name' in ${adsPropertiesFile.absolutePath}"
                    )

            val configuredStoreFile = requiredSigningProperty("storeFile")
            storeFile = adsPropertiesFile.parentFile.resolve(configuredStoreFile)
            storePassword = requiredSigningProperty("storePassword")
            keyAlias = requiredSigningProperty("keyAlias")
            keyPassword = requiredSigningProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Debug: Google test ID chính thức (an toàn, không cần secret) — giữ nguyên hardcode.
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("Boolean", "IS_ENABLE_ADMOB", "true") // false = AppLovin MAX
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713" // Google test App ID

            // QA: ép UMP báo geography EEA/NOT_EEA để xem form consent GDPR thật mà không cần VPN
            // (`./gradlew -PdebugGeo=EEA :app:installDevDebug`). Rỗng = tắt, dùng geo thật của máy.
            // Chỉ tác dụng trên thiết bị đã khai trong QA_ADMOB_TEST_DEVICE_HASHES (GalaxyApp.kt).
            val umpDebugGeoOverride = (project.findProperty("debugGeo") as String?).orEmpty()
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"$umpDebugGeoOverride\"")

            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            enableUnitTestCoverage = true
//            applicationIdSuffix = ".debug"
        }
        release {
            // Release: giá trị thật đọc từ ads.properties (private myKeyStore repo).
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${adsProp("admobBannerIdRelease")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${adsProp("admobInterstitialIdRelease")}\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"${adsProp("admobAppOpenIdRelease")}\"")
            // TODO: admobRewardedIdRelease trong ads.properties vẫn là test ID — thay khi user cấp ID prod thật.
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${adsProp("admobRewardedIdRelease")}\"")
            buildConfigField("Boolean", "IS_ENABLE_ADMOB", "true") // false = AppLovin MAX
            manifestPlaceholders["admobAppId"] = adsProp("admobAppIdRelease")

            // Release: không bao giờ ép geo — luôn dùng geography thật của user.
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"\"")

            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
        aidl = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    lint {
        abortOnError = false
        // AGP 8.7.3's bundled lint crashes with IncompatibleClassChangeError inside this specific
        // detector against our Kotlin/K2 analysis API version — a lint-tooling bug, not a real
        // finding (see doc/feature.md "signing security" entry, discovered when it also blocked
        // lintVitalAnalyzeProductionRelease on release builds). Disable until an AGP bump fixes it.
        disable += "NullSafeMutableLiveData"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    flavorDimensions.add("type")
    productFlavors {
        create("dev") {
            dimension = "type"
            resValue("string", "app_name", "Device Info & System CPU DEV")
//            resValue(
//                "string",
//                "SDK_KEY",
//                "e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt"
//            )
//            resValue("string", "BANNER", "b568752d68ca93f8")
//            resValue("string", "INTER", "5ce404d8a94fa941")
//            resValue("string", "EnableAdInter", "false")
//            resValue("string", "EnableAdBanner", "true")
        }
        create("production") {
            dimension = "type"
            resValue("string", "app_name", "Device Info & System CPU")
//            resValue(
//                "string",
//                "SDK_KEY",
//                "e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt"
//            )
//            resValue("string", "BANNER", "b568752d68ca93f8")
//            resValue("string", "INTER", "5ce404d8a94fa941")
//            resValue("string", "EnableAdInter", "true")
//            resValue("string", "EnableAdBanner", "true")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.google.material)
    implementation(libs.google.gson)
    implementation(libs.google.play.review)
    implementation(libs.google.play.review.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.glide)
    ksp(libs.glide.ksp)

    implementation(libs.work.runtime.ktx)

    implementation(libs.timber)
    implementation(libs.relinker)
    implementation(libs.coil.compose)

    implementation(libs.admob.wrapper) // replaces direct applovin-sdk + play-services-ads + ads-mediation
    implementation(libs.lottie)
    implementation(libs.mp.android.chart) // F01 realtime dashboard — pure Java, no Kotlin metadata risk

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(kotlin("test"))

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib) {
        // Pulls in an old support-library appcompat/recyclerview transitively that conflicts
        // with our AndroidX ones — only RecyclerViewActions is needed here.
        exclude(group = "com.android.support", module = "appcompat-v7")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "com.android.support", module = "recyclerview-v7")
    }
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Required at runtime because testOptions.execution = ANDROIDX_TEST_ORCHESTRATOR — without
    // these, connectedAndroidTest aborts with ClassNotFoundException: ShellMain (orchestrator
    // support APKs never installed on the device).
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
}
