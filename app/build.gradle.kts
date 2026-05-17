plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.galaxyjoy.cpuinfo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.galaxyjoy.cpuinfo"

        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
        versionCode = 20260516
        versionName = "2026.05.16"

        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }

        // AppLovin MAX
        buildConfigField("String", "APPLOVIN_SDK_KEY",     "\"e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt\"")
        buildConfigField("String", "APPLOVIN_BANNER_ID",   "\"b568752d68ca93f8\"")
        buildConfigField("String", "APPLOVIN_INTER_ID",    "\"5ce404d8a94fa941\"")
        buildConfigField("String", "APPLOVIN_APP_OPEN_ID", "\"8f218722ddc4ff48\"")
        buildConfigField("String", "APPLOVIN_REWARD_ID",   "\"e460250d026fafa6\"")

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
        create("release") {
//            val releaseSigningConfig = SigningConfig.getReleaseProperties(rootProject.rootDir)
//            storeFile = file(releaseSigningConfig.getProperty(SigningConfig.KEY_PATH))
//            keyAlias = releaseSigningConfig.getProperty(SigningConfig.KEY_ALIAS)
//            keyPassword = releaseSigningConfig.getProperty(SigningConfig.KEY_PASS)
//            storePassword = releaseSigningConfig.getProperty(SigningConfig.KEY_PASS)

            storeFile = file(project.properties["STORE_FILE"] as String)
            storePassword = project.properties["STORE_PASSWORD"] as String
            keyAlias = project.properties["KEY_ALIAS"] as String
            keyPassword = project.properties["KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("Boolean", "IS_ENABLE_ADMOB", "false") // false = AppLovin MAX

            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            enableUnitTestCoverage = true
//            applicationIdSuffix = ".debug"
        }
        release {
            //nho check APPLICATION_ID trong manifest
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3612191981543807/6633406668\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3612191981543807/5493932422\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3612191981543807/5265851653\"")
            // TODO: Thay test ID bằng AdMob Rewarded ID prod khi có (hỏi user). Test ID hiện tại
            // KHÔNG kiếm tiền — chỉ giữ để release build không crash khi rewarded button được bấm.
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("Boolean", "IS_ENABLE_ADMOB", "false") // false = AppLovin MAX

            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material)
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

    implementation(libs.epoxy)
    implementation(libs.epoxy.databinding)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.epoxy.processor)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    implementation(libs.timber)
    implementation(libs.relinker)
    implementation(libs.coil.compose)

    implementation(libs.admob.wrapper) // replaces direct applovin-sdk + play-services-ads + ads-mediation
    implementation(libs.lottie)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
}
