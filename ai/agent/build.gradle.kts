/*
 * Ryzix Code — AI Agent Module
 * Cursor-style autonomous coding assistant with tool-use loop.
 */

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
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
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  // Gemini AI SDK
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

  // OkHttp for streaming
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // Markdown rendering for agent output
  implementation("io.noties.markwon:core:4.6.2")
  implementation("io.noties.markwon:syntax-highlight:4.6.2")
}
