import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.umaia.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.umaia.android"
        minSdk = 26
        targetSdk = 36
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 8
        versionName = (findProperty("versionName") as String?) ?: "1.3.4"

        val localPropsFile = rootProject.file("local.properties")
        val props = Properties()
        if (localPropsFile.exists()) props.load(localPropsFile.inputStream())

        buildConfigField("String", "SUPABASE_URL",         "\"${props.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",    "\"${props.getProperty("SUPABASE_KEY", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${props.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "POSTHOG_API_KEY",      "\"${props.getProperty("POSTHOG_API_KEY", "")}\"")
    }

    val localPropsFile = rootProject.file("local.properties")
    val localProps = Properties().apply { if (localPropsFile.exists()) load(localPropsFile.inputStream()) }
    val keystorePath = localProps.getProperty("KEYSTORE_PATH")

    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile     = file(keystorePath)
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
                keyAlias      = localProps.getProperty("KEY_ALIAS", "")
                keyPassword   = localProps.getProperty("KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
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
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.health.connect)

    implementation(libs.posthog)
    implementation(libs.appcompat)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.paparazzi)
}

// Point test JVM at the correct Android SDK (local.properties takes precedence over ANDROID_HOME env var).
val localSdkDir: String? = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
    Properties().apply { load(f.inputStream()) }.getProperty("sdk.dir")
}
tasks.withType<Test>().configureEach {
    // Always override ANDROID_HOME so Paparazzi can locate the platform SDK.
    localSdkDir?.let { environment("ANDROID_HOME", it) }

    // Exclude screenshot tests from normal unit test runs.
    // They are included automatically when running recordPaparazziDebug / verifyPaparazziDebug.
    gradle.taskGraph.whenReady {
        val isPaparazziRun = gradle.taskGraph.allTasks.any {
            it.name.contains("Paparazzi", ignoreCase = true)
        }
        if (!isPaparazziRun && (name == "testDebugUnitTest" || name == "testReleaseUnitTest")) {
            exclude("**/screenshot/**")
        }
    }
}
