import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

fun getEnvProperty(key: String, defaultValue: String = ""): String {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        val props = Properties()
        FileInputStream(envFile).use { fis ->
            props.load(fis)
        }
        val value = props.getProperty(key)
        if (!value.isNullOrBlank()) return value.trim()
    }
    return System.getenv(key) ?: (project.findProperty(key) as? String) ?: defaultValue
}

android {
    namespace = "com.example.zyvo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.zyvo"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val zegoAppIdStr = getEnvProperty("ZEGO_APP_ID", "0")
        val zegoAppId = zegoAppIdStr.toLongOrNull() ?: 0L
        val zegoAppSign = getEnvProperty("ZEGO_APP_SIGN", "")

        buildConfigField("long", "ZEGO_APP_ID", "${zegoAppId}L")
        buildConfigField("String", "ZEGO_APP_SIGN", "\"$zegoAppSign\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.stream.webrtc)
    implementation(libs.zego.express.video)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.guava)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.google.play.services.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.mlkit.face.detection)
    debugImplementation(libs.androidx.ui.tooling)
}
