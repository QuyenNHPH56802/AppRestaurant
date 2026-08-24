plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.restaurant.staff"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.restaurant.staff"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // V2.3 / V18 — Firebase Cloud Messaging config. Values are read from
        // local.properties (which is git-ignored) and injected into BuildConfig
        // so the secret is never hard-coded. Default to "" so a build without
        // a configured Firebase project still succeeds; in that mode FCM is
        // a no-op at runtime (RestaurantFirebaseApp.isConfigured == false)
        // and only the in-app REST feed is used.
        fun localProp(key: String): String {
            val props = java.util.Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) load(f.inputStream())
            }
            return props.getProperty(key)?.takeIf { it.isNotBlank() } ?: ""
        }
        buildConfigField("String", "FCM_PROJECT_ID",     "\"${localProp("restaurant.fcm.projectId")}\"")
        buildConfigField("String", "FCM_APP_ID",         "\"${localProp("restaurant.fcm.appId")}\"")
        buildConfigField("String", "FCM_API_KEY",        "\"${localProp("restaurant.fcm.apiKey")}\"")
        buildConfigField("String", "FCM_SENDER_ID",      "\"${localProp("restaurant.fcm.messagingSenderId")}\"")
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
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)

    // V2.3 / V18 — Firebase Cloud Messaging. The BoM keeps firebase-messaging
    // and firebase-common in lockstep; we add the messaging artifact only.
    // NOTE: this module does NOT use the google-services Gradle plugin.
    // FirebaseOptions are wired manually in com.restaurant.staff.fcm.RestaurantFirebaseApp
    // using projectId / appId / apiKey that come from local.properties
    // (BuildConfig fields). This keeps google-services.json out of the repo
    // and removes one plugin conflict to manage.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // kotlinx-coroutines-play-services gives us Task<T>.await() so we can
    // call FirebaseMessaging.getToken() from a coroutine without callback glue.
    implementation(libs.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}