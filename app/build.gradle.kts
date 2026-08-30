plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ir.asgvp.ramzyar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ir.asgvp.ramzyar"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // مقادیر: gradle.properties محلی یا متغیرهای محیطی (برای GitHub Actions)
        // RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD
        val storeFilePath = (findProperty("RELEASE_STORE_FILE") as String?)
            ?: System.getenv("RELEASE_STORE_FILE")
        val storePass = (findProperty("RELEASE_STORE_PASSWORD") as String?)
            ?: System.getenv("RELEASE_STORE_PASSWORD")
        val alias = (findProperty("RELEASE_KEY_ALIAS") as String?)
            ?: System.getenv("RELEASE_KEY_ALIAS")
        val keyPass = (findProperty("RELEASE_KEY_PASSWORD") as String?)
            ?: System.getenv("RELEASE_KEY_PASSWORD")
        if (!storeFilePath.isNullOrBlank() &&
            !storePass.isNullOrBlank() &&
            !alias.isNullOrBlank() &&
            !keyPass.isNullOrBlank()
        ) {
            create("release") {
                val fromModule = file(storeFilePath)
                storeFile = if (fromModule.exists()) fromModule else rootProject.file(storeFilePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            }
        }
    }
    // فقط خروجی مخصوص معماری arm64-v8a بساز
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // DataStore برای ذخیره تنظیمات فیلتر و Regex
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Coroutines برای کارهای async (DataStore و Receiver)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}