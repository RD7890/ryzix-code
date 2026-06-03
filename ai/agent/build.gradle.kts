/*
 * Ryzix Code — AI Agent Module
 * Cursor-style autonomous coding assistant with tool-use loop.
 */

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.ryzix.agent"
  compileSdk = 34

  defaultConfig {
    minSdk = 26
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)

  // Coroutines — correct catalog alias
  implementation(libs.common.kotlin.coroutines.android)

  // Lifecycle — no catalog entry; pinned directly
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

  // OkHttp — used to call Gemini REST API (catalog entry exists)
  implementation(libs.okhttp)

  // Markdown rendering for agent chat panel
  implementation("io.noties.markwon:core:4.6.2")
}
