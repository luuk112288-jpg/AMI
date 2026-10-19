plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.ami.music"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ami.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    implementation(libs.core)
    implementation(libs.activity)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.icons)
    implementation(libs.compose.preview)
    implementation(libs.media3.player)
    implementation(libs.media3.session)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.android.test)
    androidTestImplementation(libs.android.runner)
    androidTestImplementation(libs.room.test)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
