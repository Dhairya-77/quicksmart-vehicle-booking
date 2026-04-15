import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)  // processes google-services.json → auto-inits Firebase
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.quicksmart.android"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.quicksmart.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsApiKey = localProperties.getProperty("mapKey") ?: ""
        val paymentAppKey = localProperties.getProperty("paymentAppKey") ?: ""
        val paymentSecretKey = localProperties.getProperty("paymentSecretKey") ?: ""

        manifestPlaceholders["mapKey"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "CASHFREE_APP_KEY", "\"$paymentAppKey\"")
        buildConfigField("String", "CASHFREE_SECRET_KEY", "\"$paymentSecretKey\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //reference from libs
    implementation(libs.sdp)

    // Maps, Places & Location
    implementation(libs.google.maps)
    implementation(libs.google.places)
    implementation(libs.play.services.location)

    // Firebase BoM (controls all versions)
    implementation(platform(libs.firebase.bom))

    // Firebase SDKs
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)

    // Image loading
    implementation(libs.glide)
    implementation(libs.circleimageview)

    // Cashfree Payment Gateway SDK
    implementation("com.cashfree.pg:api:2.3.2")

    // OkHttp for creating Cashfree orders from client (sandbox)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}