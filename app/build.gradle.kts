import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val enableGoogleServices = (project.findProperty("enableGoogleServices") ?: "false").toString().toBoolean()

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    logger.warn("keystore.properties file not found at: ${keystorePropertiesFile.absolutePath}")
}

android {
    namespace = "com.example.webgame"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.webgame"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders.putAll(mapOf(
            //appKey，需要与控制台上的一样，与packageName是一对一关系
            "ENGAGELAB_PRIVATES_APPKEY" to "b1a61e2fe4004a419c7f996c",
            // Engagelab appChannel，用于渠道统计
            "ENGAGELAB_PRIVATES_CHANNEL" to "developer",
            // Engagelab process，Engagelabsdk工作所在的进程，注意:开头
            "ENGAGELAB_PRIVATES_PROCESS" to ":remote"
        ))
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a") // 只打 arm64 架构包
            isUniversalApk = false // 不输出全架构包
        }
    }

    signingConfigs {
        create("release") {
            if (System.getenv()["CI"] == "true") {
                storeFile = System.getenv()["CM_KEYSTORE_PATH"]?.let { file(it) }
                storePassword = System.getenv()["CM_KEYSTORE_PASSWORD"]
                keyAlias = System.getenv()["CM_KEY_ALIAS"]
                keyPassword = System.getenv()["CM_KEY_PASSWORD"]
            } else {
                keyAlias = keystoreProperties.getProperty("keyAlias", "kkgame")
                keyPassword = keystoreProperties.getProperty("keyPassword", "123456")
                storeFile = file(keystoreProperties.getProperty("storeFile", "./release.jks"))
                storePassword = keystoreProperties.getProperty("storePassword", "123456")
            }
            enableV1Signing = true
            enableV2Signing = true
        }
        getByName("debug") {
            keyAlias = keystoreProperties.getProperty("keyAlias", "kkgame")
            keyPassword = keystoreProperties.getProperty("keyPassword", "123456")
            storeFile = file(keystoreProperties.getProperty("storeFile", "./release.jks"))
            storePassword = keystoreProperties.getProperty("storePassword", "123456")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isZipAlignEnabled = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)

    // 协程
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Gson for JSON parsing
    implementation(libs.gson)
    // 导入x系列的 webkit 库，
    // 此库提供的文档级注入，在每次页面开始解析、所有业务 JS 执行前自动注入，刷新 / 跳转都自动重跑，完美解决 “注入时机太早 / 太晚” 问题
    implementation(libs.androidx.webkit)
    // 极光推送 国际版
    implementation(libs.engagelab)
    // 极光推送 国际版，sound语音播报功能
    implementation(libs.oth.sound)
    if (enableGoogleServices) {
        // 极光推送 国际版，谷歌服务插件包
        implementation(libs.engagelab.google)
        implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
        implementation("com.google.firebase:firebase-messaging")
    }
}

if (enableGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
}

// 自动复制混淆 mapping 到指定目录
tasks.whenTaskAdded {
    if (name == "assembleRelease") {
        doLast {
            val mappingFile = file("build/outputs/mapping/release/mapping.txt")
            val targetDir = file("../split-debug-info")
            if (mappingFile.exists()) {
                targetDir.mkdirs()
                mappingFile.copyTo(file("$targetDir/mapping.txt"), overwrite = true)
            }
        }
    }
}